package Googol.Barrel;

import Googol.Downloader.Downloader;
import Googol.Manager.BarrelManager.BarrelManagerInterface;
import Googol.Multicast.MulticastMessage;
import Googol.Multicast.Receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * Class that handles barrels
 */

public class Barrel extends UnicastRemoteObject implements BarrelInterface{
    /**
     * Receives MulticastMessages
     */
    private final Receiver receiver;
    /**
     * Manages all barrels
     */
    BarrelManagerInterface barrelManager;
    /**
     * Remissive Index
     */
    private final RemissiveIndex remissiveIndex;
    /**
     * This barrel's ID
     */
    private final int myID;
    /**
     * This barrel's port
     */
    private final int barrelPort;
    /**
     * Returns true if multicast is available
     */

    public int activeInstances;
    /**
     * Boolean that shows is multicast is still available
     */
    public boolean multicastAvailable = true; // Initially assume multicast group is available
    /**
     * This barrel's address
     */
    private final String myAddress;

    /**
     * Class constructer, attributes are initialized, RMI connection to BarrelManager (inside Gateway) is established
     * @param multicastAddress Googol.Multicast address
     * @param port             Port for receiving multicast messages.
     * @param confirmationPort Port for sending confirmation messages.
     * @param gatewayAddress   Gateway address and port where the BarrelManager is located
     * @param myID     Barrel number (id)
     * @param barrelPort       Barrel port
     * @throws IOException If the operation is interrupted
     * @throws NotBoundException Remote object is not bound to the specified name in the registry.
     * @throws InterruptedException If the operation is interrupted
     */
    public Barrel(String multicastAddress, int port, int confirmationPort, String gatewayAddress, int myID, int barrelPort) throws IOException, NotBoundException, InterruptedException {
        this.myID = myID;
        this.barrelPort = barrelPort;
        bindMyself();
        connectToBarrelManagerInGateway(gatewayAddress, 4);
        this.myAddress = getMyAddress();
        verifyMyID(this.myAddress);
        this.remissiveIndex = barrelManager.setRemissiveIndex(this.myID);
        Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
        this.receiver = new Receiver(multicastAddress, port, confirmationPort);
        //start fetching active instances
        ActiveInstancesFetcher activeInstancesFetcher = new ActiveInstancesFetcher();
        activeInstancesFetcher.startFetching();
        System.out.println("[BARREL#" + myID + "]:" + "   Ready...");
    }
    /**
     * Method that verifies if the barrel's ID is valid
     * @param registryAddress Barrel's address
     * @throws RemoteException If a remote communication error occurs.
     */
    private void verifyMyID(String registryAddress) throws RemoteException {
        if(!this.barrelManager.verifyID(this.myID,registryAddress, this.barrelPort)){
            System.out.println("[BARREL#" + myID + "]:" + "   Barrel ID is not valid. Exiting...");
            System.exit(1);
        }
    }
    /**
     * Method that binds this barrel to the RMI registry
     */
    private void bindMyself(){
        try {
            Registry registry = LocateRegistry.createRegistry(this.barrelPort);
            registry.rebind("barrel" + this.myID, this);

        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Remote Exception Error\n "+ e.getMessage(), e);
            exit();
        }
    }
    /**
     * Method that connects to the BarrelManager in the Gateway
     * @param gatewayAddress Gateway address
     * @throws InterruptedException If the operation is interrupted
     */
    private void connectToBarrelManagerInGateway(String gatewayAddress, int numTries) throws InterruptedException {
        if (numTries == 0) {
            System.out.println("[BARREL#" + myID + "]:" + "   Could not connect to BarrelManager. Exiting...");
            System.exit(1);
        }
        try {

            this.barrelManager = (BarrelManagerInterface) Naming.lookup(gatewayAddress);
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            sleep(500);
            --numTries;
            System.out.println("[Barrel#"+ myID +"]: Connecting to Gateway(BarrelManager) " +numTries + " tries left");
            connectToBarrelManagerInGateway(gatewayAddress, --numTries);
        }
    }
    /**
     * Logger to print error messages
     */
    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());

    /**
     * Method that returns the Barrel number (id)
     * @return Barrel Number
     * @throws RemoteException If a remote communication error occurs.
     */
    public int getMyID() throws RemoteException {
        return this.myID;
    }

    /**
     * Method that returns the running machine address
     * @return running machine address
     * @throws UnknownHostException Hostname provided is unknown or could not be resolved.
     */
    private String getMyAddress() throws UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();
        return address.getHostAddress();
    }

    /**
     * Method that receives MulticastMessage of CITATION type and inserts it in its hyperlink's position in the remissive index
     * @param message MulticastMessage of CITATION type
     */
    private void receiveCitation(MulticastMessage message) {
        String hyperlink = message.hyperlink();
        String citation = message.payload();
        remissiveIndex.insertWebPageCitation(hyperlink, citation);
    }

    /**
     * Method that receives MulticastMessage of TITLE type and inserts it in its hyperlink's position in the remissive index
     * @param message MulticastMessage of TITLE type
     */
    private void receiveTitle(MulticastMessage message) {
        String hyperlink = message.hyperlink();
        String title = message.payload();
        remissiveIndex.insertWebPageTitle(hyperlink, title);
    }

    /**
     * Method that receives MulticastMessage of TOKEN type and inserts them in its hyperlink's position in the remissive index
     * @param message MulticastMessage of TOKEN type
     */
    private void receiveTokens(MulticastMessage message) {
        StringTokenizer tokens = getTokens(message.payload(), " ");
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken();
            remissiveIndex.addIndex(message.hyperlink(), token.trim());
        }
    }

    /**
     * Method that receives MulticastMessage of CONNECTIONS type and inserts them in its hyperlink's position in the remissive index
     * @param message MulticastMessage of TOKEN type
     */
    private void receiveConnections(MulticastMessage message){
        StringTokenizer mentionsURLS = getTokens(message.payload(), "^");
        while (mentionsURLS.hasMoreElements()) {
            String url = mentionsURLS.nextToken();
            remissiveIndex.addURLConnections(url, message.hyperlink());
        }
    }

    /**
     * Method that receives a String of tokens and inserts them in a StringTokenizer
     * @param message MulticastMessage of TOKEN or CONNECTIONS type
     * @param delimiter delimiter between tokens
     * @return StringTokenizer of tokens
     */
    private StringTokenizer getTokens(String message, String delimiter) {
        return new StringTokenizer(message, delimiter);
    }

    /**
     * Method that returns this barrel's RemissiveIndex
     * @return RemissiveIndex
     * @throws RemoteException If a remote communication error occurs.
     */
    public RemissiveIndex getRemissiveIndex() throws RemoteException{
        System.out.println("[BARREL#" + myID + "]:" + "   Returning remissive index");
        return this.remissiveIndex;
    }

    /**
     * Method that performs a search based on given tokens and returns the websites that contain them
     * After Webpages are found, they are ordered
     * @param tokens Tokens to search for in the RemissiveIndex's keyset
     * @param pageNumber Page number (each page contains 10 results)
     * @param intersection If true, returns only pages that contain all tokens. If false, returns only pages that contain each token
     * @return Array of 10 websites, according to the requested page
     * @throws RemoteException If a remote communication error occurs.
     */
    public WebPage[] search(String[] tokens, Integer pageNumber, boolean intersection) throws RemoteException{
        LinkedList<WebPage> result;
        if(intersection)
            result = remissiveIndex.findWebPagesIntersection(tokens);
        else
            result = remissiveIndex.findWebPagesUnion(tokens);
        if(result == null || result.isEmpty())
            return new WebPage[0];
        //System.out.println(result.toString());
        orderWebpages(result);
        //System.out.println(result.toString());
        if(result.size() < pageNumber * 10)
            return result.toArray(new WebPage[0]);
        return result.subList(Math.min(result.size(),pageNumber * 10), Math.min(pageNumber * 10 + 10, result.size())).toArray(new WebPage[0]);
    }

    /**
     * Method that orders a linkedlist of Webpages based on its number of connections
     * @param result ordered linkedlist of Webpages
     */
    public void orderWebpages(LinkedList<WebPage> result) {
        // Define a custom comparator based on the length of the attribute in descending order
        Comparator<WebPage> comparator = (page1, page2) -> {
            String url1 = page1.getHyperlink();
            String url2 = page2.getHyperlink();

            int connections1 = remissiveIndex.getNumberOfConnections(url1);
            int connections2 = remissiveIndex.getNumberOfConnections(url2);

            // Compare the lengths of the attribute in each Googol.Barrel.WebPage in reverse order
            return Integer.compare(connections2, connections1); // Compare in reverse order
        };

        // Sort the result list using the custom comparator
        result.sort(comparator);
    }

    /**
     * Method that returns a given Webpage's connections
     * @param URL Webpage
     * @return number of connections
     * @throws RemoteException If a remote communication error occurs.
     */
    public String getConnections(String URL) throws RemoteException{
        String result = remissiveIndex.getConnections(URL);
        if(result == null) {
            System.out.println("[BARREL#" + myID + "]:" + "   URL not found");
            return "";
        }
        return result;
    }

    /**
     * Method that performs Barrel's operations while it's running
     * While the Googol.Multicast connection is available, it keeps receiving messages related to a URL's data
     * It analyzes that data and inserts it in the Remissive Index
     */
    public void run() {
        try {
            while (multicastAvailable) {
                //remissiveIndex.printIndexHashMap(barrelNumber);
                MulticastMessage message = receiver.receiveMessage(activeInstances);
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
            }
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            exit();
        } finally {
            multicastAvailable = false;
            exit();

        }
    }

    /**
     * Method called when Barrel stops running
     * It has BarrelManager remove this downloader from the list of active barrels when it stops running
     */

    private void exit() {
        try {
            this.barrelManager.removeInstance(myAddress, this.barrelPort,this.myID);
            receiver.close();
            System.out.println("[BARREL#" + myID + "]:" + "   Exiting...");
        }
        catch(Exception e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }
    }
    /**
     * Class that fetches active instances
     */
    public class ActiveInstancesFetcher extends TimerTask {
        /**
         * Interval to fetch active instances
         */
        private final int interval = 30 * 1000; // 30 seconds
        /**
         * Class constructor
         */
        public ActiveInstancesFetcher() {
        }
        /**
         * Method that fetches active instances
         */
        @Override
        public void run() {
            try {
                activeInstances = barrelManager.getActiveInstances();
            } catch (RemoteException e) {
                LOGGER.log(Level.SEVERE, "Error\n" + e.getMessage(), e);
                exit();
            }
        }
        /**
         * Method that starts fetching active instances
         */
        public void startFetching() {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(this, 0, interval);
        }
    }


    /**
     * Main method that initializes a Barrel
     * @param args Arguments to initialize a Barrel
     * @throws IOException If the operation is interrupted
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 7) {
            System.out.println("Usage: java Barrel <multicastAddress> <port> <confirmationPort> <gatewayAdress> <barrelManagerPort> <barrelPort>");
            System.exit(1);
        }
        try {
            String gatewayAdress = "rmi://" + args[3] + ":" + args[4] + "/gateway";
            Barrel barrel = new Barrel( args[0],  Integer.parseInt(args[1]),Integer.parseInt(args[2]), gatewayAdress, Integer.parseInt(args[5]), Integer.parseInt(args[6]));
            barrel.run();
        } catch (RemoteException | NotBoundException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error\n"+ e.getMessage(), e);
        }
    }
}