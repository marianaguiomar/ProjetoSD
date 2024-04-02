package Googol.Gateway;

import Googol.Barrel.BarrelInterface;
import Googol.ProjectManager.ProjectManagerInterface;
import Googol.Queue.QueueInterface;
import Googol.Barrel.WebPage;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Gateway extends UnicastRemoteObject implements GatewayInterface{
    BarrelInterface barrel;
    QueueInterface queue;

    private final String queuePath;
    //1100
    public int barrelInUse;
    ProjectManagerInterface projectManager;
    private final HashMap<Integer,Long> totalDuration;
    private final HashMap<Integer, Integer> numSearches;
    private final LinkedHashMap<String, Integer> searches;



    public Gateway(Registry registry, String queuePath, String projectManagerPath) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.barrelInUse = 1;
        this.queuePath = queuePath;
        this.totalDuration = new HashMap<>();
        this.numSearches = new HashMap<>();
        this.searches = new LinkedHashMap<>();
        this.projectManager = (ProjectManagerInterface) Naming.lookup(projectManagerPath);
        registry.rebind("gateway", this);
    }
    private void connectToQueue(){
        try{
            this.queue = (QueueInterface) Naming.lookup(queuePath);
        }
        catch(NotBoundException notBoundException){
            System.out.println("[GATEWAY]: Queue not found");
        }
        catch (RemoteException remoteException){
            System.out.println("[GATEWAY]: Remote Exception in Queue");
        }
        catch (MalformedURLException malformedURLException){
            System.out.println("[GATEWAY]: Malformed URL Exception in Queue");
        }
    }

    private void connectToBarrel() throws RemoteException {
        try{
            int barrelPort = 4400 + projectManager.getBarrelID(barrelInUse);
            System.out.println("[GATEWAY]: Connecting to barrel number "+ projectManager.getBarrelID(barrelInUse));
            this.barrel = (BarrelInterface) Naming.lookup("rmi://localhost:" + barrelPort + "/barrel" + projectManager.getBarrelID(barrelInUse));
        }
        catch(NotBoundException notBoundException){
            System.out.println("[GATEWAY]: Barrel number "+ projectManager.getBarrelID(barrelInUse) +" not found. Trying next barrel...");
            barrelInUse = (barrelInUse + 1) % (this.projectManager.getNumberOfBarrels());
            connectToBarrel();
        }
        catch (RemoteException remoteException){
            System.out.println("[GATEWAY]: Remote Exception");
        }
        catch (MalformedURLException malformedURLException){
            System.out.println("[GATEWAY]: Malformed URL Exception");
        }
    }
    public void run(){
        try {
            connectToQueue();
            connectToBarrel();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[GATEWAY]: Gateway Ready...");
    }
    public void updateSearches(long duration){
        if (!totalDuration.containsKey(barrelInUse)) {
            totalDuration.put(barrelInUse, duration);
        }
        else {
            long currDuration = totalDuration.get(barrelInUse);
            long updatedDuration = currDuration + duration;
            totalDuration.put(barrelInUse, updatedDuration);
        }

        if (!numSearches.containsKey(barrelInUse)) {
            numSearches.put(barrelInUse, 1);
        }
        else {
            long currSearches = numSearches.get(barrelInUse);
            long updatedSearches = currSearches + 1;
            totalDuration.put(barrelInUse, updatedSearches);
        }
    }
    public String search(String[] tokens, int pageNumber, boolean interstionSearch) throws RemoteException {
        //TODO -> verificar onde colocar os temporizadores
        long startTime = System.currentTimeMillis();
        System.out.println("[GATEWAY]: Searching for: " + tokens[0]);
        WebPage[] webPages = barrel.search(tokens, pageNumber, interstionSearch);
        System.out.println("[GATEWAY]: Search done");

        long endTime = System.currentTimeMillis();
        updateSearches(endTime-startTime);

        StringBuilder result= new StringBuilder(webPages.length + " RESULTADOS PARA A PÁGINA " + pageNumber + "\n");
        for (WebPage webPage : webPages) {
            result.append(webPage.toString()).append("\n");
        }

        String currKey = String.join(" ", tokens).toLowerCase();
        if (searches.containsKey(currKey)) {
            int curr = searches.get(currKey);
            searches.put(currKey, curr+1);
        }
        else {
            searches.put(currKey, 1);
        }
        updateSearches();

        return result.toString();
    }

    //TODO -> fix
    public String status() throws RemoteException {
        HashSet<Integer> barrelID = new HashSet<>();
        // Get the original status message from the barrel
        //String[] splitRes = barrel.status().split("§");

        String topSearches = formatSearches();
        // Format the HashSet into a string
        StringBuilder activeBarrels = new StringBuilder();
        activeBarrels.append("\nACTIVE BARRELS \n");
        for (int value : projectManager.getBarrelsID()) {
            activeBarrels.append("BARREL#").append(value).append("\n");
        }

        // Calculate average search time
        String averageTimeMessage = formatAverageTime();

        // Combine the original status message and the average time message

        // Return the combined message
        return topSearches + "\n" + averageTimeMessage + "\n" + activeBarrels + "\n";
    }
    public void insert(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        System.out.println("[GATEWAY]: Inserting URL: " + URL);
        this.queue.addURL(URL);
    }

    public String formatSearches() {
        StringBuilder result = new StringBuilder();
        int count = 1;
        result.append("TOP 10 SEARCHES \n");
        for (Map.Entry<String, Integer> entry : searches.entrySet()) {
            if (count > 10) {
                break; // Stop when you've printed the first 10 entries
            }
            result.append("[").append(count).append("] ").append(entry.getKey()).append("\n");
            count++;
        }
        return result.toString();
    }

    private void updateSearches() {
        // Convert map entries to a list
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(searches.entrySet());

        // Sort the list based on values in descending order using a lambda expression
        entryList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        // Create a new LinkedHashMap to preserve the order
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Sorted list: " + entryList);
        //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Sorted map: " + sortedMap);

        // Update the original map with the sorted entries
        searches.clear();
        searches.putAll(sortedMap);

        //System.out.println("[BARREL#" + barrelNumber + "]:" + "    Searches: " + searches);
    }

    public String formatAverageTime() {
        StringBuilder result = new StringBuilder();
        result.append("AVERAGE SEARCH TIME: \n");
        //TODO -> quando ainda não existem searches -> verificar se funciona, outro barrel n faz nada
        for (Integer barrel: totalDuration.keySet()) {
            long averageDuration = totalDuration.get(barrel) / numSearches.get(barrel);
            result.append("BARREL#").append(barrel).append(": ").append(averageDuration).append(" ms.");
        }

        return result.toString();
    }

    public String getConnections(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        String result = "Resultado: \n";
        System.out.println("[GATEWAY]: Getting connections for URL: " + URL);
        result = result.concat(barrel.getConnections(URL));

        System.out.println(result);

        return result;
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        //"rmi://localhost:1099/queue", "rmi://localhost:" + 4320 + "/projectManager"
        if(args.length != 4){
            System.out.println("Usage: java Gateway <queueIP> <queuePort> <projectManagerIP> <projectManagerPort>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            String queueAddress = "rmi://" + args[0] + ":" + args[1] + "/queue";
            String projectManagerAddress = "rmi://" + args[2] + ":" + args[3] + "/projectManager";
            Registry registry = LocateRegistry.createRegistry(1100);
            Gateway gateway = new Gateway( registry,
                    queueAddress, projectManagerAddress);
            gateway.run();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }



}