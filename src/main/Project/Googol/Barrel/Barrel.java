package Googol.Barrel;
import Googol.Downloader;
import Googol.ProjectManager.ProjectManagerInterface;
import Multicast.MulticastMessage;
import Multicast.Receiver;
import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Barrel extends UnicastRemoteObject implements BarrelInterface, Runnable{
    // TODO -> see static variable
    private final Receiver receiver;
    ProjectManagerInterface projectManager;
    private final RemissiveIndex remissiveIndex;
    private final int barrelNumber;

    private final int barrelPort;

    String pathToHere = "./src/main/Project/Googol/Barrel/";
    boolean multicastAvailable = true; // Initially assume multicast group is available

    public Barrel(String multicastAddress, int port, int confirmationPort, String projectManagerPath, int barrelNumber) throws IOException, NotBoundException {
        //this.remissiveIndex = initializeRemissiveIndex(pathToHere + "backup" + barrelNumber);

        this.projectManager = (ProjectManagerInterface) Naming.lookup(projectManagerPath);
        this.barrelNumber = barrelNumber;
        if(!this.projectManager.verifyBarrelID(this.barrelNumber)){
            System.out.println("[BARREL#" + barrelNumber + "]:" + "   Barrel ID is not valid. Exiting...");
            System.exit(1);
        }
        this.remissiveIndex = projectManager.setRemissiveIndex(this.barrelNumber);
        Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
        this.receiver = new Receiver(multicastAddress, port, confirmationPort);
        this.barrelPort = 4400 + this.barrelNumber;
        try {
            Registry registry = LocateRegistry.createRegistry(barrelPort);
            registry.rebind("barrel" + barrelNumber, this);

        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "slay pussy boss queen\n "+ e.getMessage(), e);
            exit();
        }
        System.out.println("[BARREL#" + barrelNumber + "]:" + "   Ready...");
    }

    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());
    
    private void receiveCitation(MulticastMessage message) {
        String hyperlink = message.hyperlink();
        String citation = message.payload();
        remissiveIndex.insertWebPageCitation(hyperlink, citation);
    }
    private void receiveTitle(MulticastMessage message) {
        String hyperlink = message.hyperlink();
        String title = message.payload();
        remissiveIndex.insertWebPageTitle(hyperlink, title);
    }

    private StringTokenizer getTokens(String message, String delimiter) {
        return new StringTokenizer(message, delimiter);
    }
    public RemissiveIndex getRemissiveIndex() throws RemoteException{
        System.out.println("[BARREL#" + barrelNumber + "]:" + "   Returning remissive index");
        return this.remissiveIndex;
    }
    public WebPage[] search(String[] tokens, Integer pageNumber, boolean intersection) throws RemoteException{
        LinkedList<WebPage> result;
        if(intersection)
            result = remissiveIndex.findWebPagesIntersection(tokens);
        else
            result = remissiveIndex.findWebPagesUnion(tokens);
        if(result == null || result.isEmpty())
            return new WebPage[0];
        orderWebpages(result);
        //updateSearches();

        if(result.size() < pageNumber * 10)
            return new WebPage[0];
        return result.subList(pageNumber * 10, Math.min(pageNumber * 10 + 10, result.size())).toArray(new WebPage[0]);
    }

    public void orderWebpages(LinkedList<WebPage> result) {
        // Define a custom comparator based on the length of the attribute in descending order
        Comparator<WebPage> comparator = new Comparator<>() {
            @Override
            public int compare(WebPage page1, WebPage page2) {
                String url1 = page1.getHyperlink();
                String url2 = page2.getHyperlink();

                int connections1 = remissiveIndex.getNumberOfConnections(url1);
                int connections2 = remissiveIndex.getNumberOfConnections(url2);

                // Compare the lengths of the attribute in each Googol.Barrel.WebPage in reverse order
                return Integer.compare(connections2, connections1); // Compare in reverse order
            }
        };

        // Sort the result list using the custom comparator
        Collections.sort(result, comparator);
    }



    public String getConnections(String URL) throws RemoteException{
        return remissiveIndex.getConnections(URL);
    }



    private void receiveTokens(MulticastMessage message) {
        StringTokenizer tokens = getTokens(message.payload(), " ");
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken();
            remissiveIndex.addIndex(message.hyperlink(), token.trim());
        }
    }

    private void receiveConnections(MulticastMessage message){
        StringTokenizer mentionsURLS = getTokens(message.payload(), "^");
        while (mentionsURLS.hasMoreElements()) {
            String url = mentionsURLS.nextToken();
            remissiveIndex.addURLConnections(url, message.hyperlink());
        }
    }
    public void run() {
        try {
            while (multicastAvailable) {
                MulticastMessage message = receiver.receiveMessage();
                if(message == null){
                    continue;
                }
                //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Received message: " + message.messageType() + " " + message.payload());
                switch (message.messageType()){
                    case TITLE -> //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Received TITLE message: " + message.payload());
                            receiveTitle(message);
                    case CITATION -> //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Received CITATION message: " + message.payload());
                            receiveCitation(message);
                    case TOKENS -> //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Received TOKENS message: " + message.payload());
                            receiveTokens(message);
                    case CONNECTIONS -> //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Received CONNECTIONS message: " + message.payload());
                            receiveConnections(message);
                }
                //remissiveIndex.printIndexHashMap(barrelNumber);
            }
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        } finally {
            multicastAvailable = false;
            exit();

        }
    }

    private void exit() {
        try {
            //TODO -> send real address
            this.projectManager.removeBarrel("localhost", this.barrelPort,this.barrelNumber);
        }
        catch(RemoteException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 6) {
            System.out.println("Usage: java Barrel <multicastAddress> <port> <confirmationPort> <projectManagerAddress> <projectManagerPort>");
            System.exit(1);
        }
        try {
            String projectManagerAddress = "rmi://" + args[3] + ":" + args[4] + "/projectManager";
            Barrel barrel = new Barrel( args[0],  Integer.parseInt(args[1]),Integer.parseInt(args[2]), projectManagerAddress, Integer.parseInt(args[5]));
            barrel.run();
        } catch (RemoteException | NotBoundException e) {
            LOGGER.log(Level.SEVERE, "slayyy"+ e.getMessage(), e);
        }
    }
}