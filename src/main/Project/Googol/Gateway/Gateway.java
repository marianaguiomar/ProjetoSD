package Googol.Gateway;
import Googol.Barrel.BarrelInterface;
import Googol.Barrel.WebPage;
import Googol.Manager.BarrelManager.BarrelManager;
import Googol.Queue.QueueInterface;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static java.lang.Thread.sleep;

/**
 * Class that manages the Gateway
 */
public class Gateway extends UnicastRemoteObject implements GatewayInterface{
    /**
     * Barrel interface
     */
    BarrelInterface barrel;

    /**
     * Queue interface
     */
    QueueInterface queue;

    /**
     * Queue path
     */
    private final String queuePath;
    //1100

    /**
     * Barrel that's being used
     */
    public int barrelInUse;
    /**
     * Barrel manager
     */
    BarrelManager barrelManager;

    /**
     * Hash map that keeps the total duration of searches performed by a barrel
     */
    private final HashMap<Integer,Double> totalDuration;

    /**
     * Hash map that keeps the number of searches performed by a barrel
     */
    private final HashMap<Integer, Integer> numSearches;

    /**
     * Hash map that keeps the searches performed
     */
    private final LinkedHashMap<String, Integer> searches;


    /**
     * Class constructer, attributes are initialized
     * @param registry RMI registry
     * @param queuePath Queue path
     * @param barrelManagerPort Barrel manager port
     * @param whitelistPath Path to the barrel's whitelist
     * @param backupPath Path to back up file
     * @throws RemoteException If a remote communication error occurs.
     */
    public Gateway(Registry registry, String queuePath, int barrelManagerPort, String whitelistPath, String backupPath) throws RemoteException {
        super();
        Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
        this.barrelInUse = 0;
        this.queuePath = queuePath;
        this.totalDuration = new HashMap<>();
        this.numSearches = new HashMap<>();
        this.searches = new LinkedHashMap<>();
        connectToQueue(3);
        this.barrelManager = new BarrelManager(barrelManagerPort,whitelistPath,
                backupPath, this.queue);
        registry.rebind("gateway", this);
    }

    /**
     * Method that connects the Gateway to the Queue
     */
    private void connectToQueue(int numTries){
        try{
            this.queue = (QueueInterface) Naming.lookup(queuePath);
        }
        catch(NotBoundException notBoundException){
            --numTries;
            System.out.println("[GATEWAY]: Connecting to queue " +numTries + " tries left");
            System.out.println("[GATEWAY]: Queue not found.");
            connectToQueue(numTries);
        }
        catch (RemoteException remoteException){
            --numTries;
            System.out.println("[GATEWAY]: Connecting to queue " +numTries + " tries left");
            System.out.println("[GATEWAY]: Remote Exception in Queue");
            connectToQueue(numTries);
        }
        catch (MalformedURLException malformedURLException){
            --numTries;
            System.out.println("[GATEWAY]: Connecting to queue " +numTries + " tries left");
            System.out.println("[GATEWAY]: Malformed URL Exception in Queue");
            connectToQueue(numTries);
        }
    }

    /**
     * Method that connects the Gateway to a barrel and balance workload between barrels (RoundRobin)
     * @throws RemoteException If a remote communication error occurs.
     */
    private void connectToBarrel() throws RemoteException {
        if(this.barrelManager.getActiveInstances() == 0){
            System.out.println("[GATEWAY]: No barrels available");
            return;
        }

        try{
            if (++barrelInUse > this.barrelManager.getActiveInstances()) {
                barrelInUse = 0;
            }
            int barrelID = this.barrelManager.getAvailableBarrel(barrelInUse);
            System.out.println("[GATEWAY]: Connecting to barrel number "+ barrelID );
            this.barrel = this.barrelManager.lookupBarrel(barrelID);
        } catch (RemoteException remoteException){
            System.out.println("[GATEWAY]: Remote Exception");
        }
    }
    /**
     * Method that exits the Gateway
     */
    private void exit() {
        try {
            //Sleep to allow the barrel to send the Remissive Index
            sleep(1000);
            System.out.println("[GATEWAY]: Exiting...");
        } catch (Exception e) {
            System.out.println("[GATEWAY]: Error while exiting");
        }
    }

    /**
     * Method that updates searches performed (by each barrel and in total)
     * @param duration Time it took to perform a search
     * @throws RemoteException If a remote communication error occurs.
     */
    public void updateSearches(double duration) throws RemoteException{
        if (!totalDuration.containsKey(this.barrel.getMyID())) {
            totalDuration.put(this.barrel.getMyID(), duration);
        }
        else {
            double currDuration = totalDuration.get(this.barrel.getMyID());
            double updatedDuration = currDuration + duration;
            totalDuration.put(this.barrel.getMyID(), updatedDuration);
        }
        if (!numSearches.containsKey(this.barrel.getMyID())) {
            numSearches.put(this.barrel.getMyID(), 1);
        }
        else {
            double currSearches = numSearches.get(this.barrel.getMyID());
            double updatedSearches = currSearches + 1;
            totalDuration.put(this.barrel.getMyID(), updatedSearches);
        }
    }

    /**
     * Method that performs a search (communicating with the Barrels)
     * @param tokens okens to search for
     * @param pageNumber Page number (each page contains 10 results)
     * @param isIntersectionSearch If true, intersection. If false, union
     * @return Set of 10 websites, according to the requested page
     * @throws RemoteException If a remote communication error occurs.
     */
    public String search(String[] tokens, int pageNumber, boolean isIntersectionSearch) throws RemoteException {
        long startTime = System.currentTimeMillis();
        System.out.println("[GATEWAY]: Searching for: " + tokens[0]);
        WebPage[] webPages;
        try {
            connectToBarrel();
            webPages = barrel.search(tokens, pageNumber, isIntersectionSearch);
        }
        catch (RemoteException e) {
            int counter = 0;
            do {
                connectToBarrel();
                webPages = barrel.search(tokens, pageNumber, isIntersectionSearch);
                counter++;
            }
            while (webPages == null && counter < this.barrelManager.getActiveInstances());

        }
        System.out.println("[GATEWAY]: Search done");

        long endTime = System.currentTimeMillis();
        updateSearches((double) endTime-startTime);
        assert webPages != null;
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
        orderSearches();

        return result.toString();
    }

    /**
     * Method that returns the administrator informatin of the system (top10 searches performed,
     * average duration of search per barrel, active barrels
     * @return top10 searches performed, average duration of search per barrel, active barrels
     * @throws RemoteException If a remote communication error occurs.
     */
    public String status() throws RemoteException {
        String topSearches = formatSearches();
        StringBuilder activeBarrels = new StringBuilder();
        activeBarrels.append("ACTIVE BARRELS \n");
        for (int value : this.barrelManager.getAvailableBarrelsID()) {
            activeBarrels.append("BARREL#").append(value).append("\n");
        }

        // Calculate average search time
        String averageTimeMessage = formatAverageTime();

        // Return the combined message
        return topSearches + "\n" + averageTimeMessage + "\n" + activeBarrels + "\n";
    }

    /**
     * Method that inserts a URL in the URLQueue
     * @param URL URL to be inserted
     * @throws RemoteException If a remote communication error occurs.
     */
    public void insert(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        System.out.println("[GATEWAY]: Inserting URL: " + URL);
        this.queue.addURL(URL);
    }

    /**
     * Method that formats the top10 searches
     * @return Formatted string with the top10 searches
     */
    public String formatSearches() {
        StringBuilder result = new StringBuilder();
        int count = 1;
        result.append("TOP 10 SEARCHES \n");
        for (Map.Entry<String, Integer> entry : searches.entrySet()) {
            if (count > 10) {
                break;
            }
            result.append("[").append(count).append("] ").append(entry.getKey()).append("\n");
            count++;
        }
        return result.toString();
    }

    /**
     * Method that orders the searches based on the number of times they were searched
     */
    private void orderSearches() {
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

    /**
     * Method that formats the average time a barrel takes to perform a search
     * @return Formatted string with the average time a barrel takes to perform a search
     */
    public String formatAverageTime() {
        StringBuilder result = new StringBuilder();
        result.append("AVERAGE SEARCH TIME: \n");
        for (Integer barrel: totalDuration.keySet()) {
            double averageDuration = (totalDuration.get(barrel)  / (double) numSearches.get(barrel)) * 0.01;
            result.append("BARREL#").append(barrel).append(": ").append(averageDuration).append("\n");
        }

        return result.toString();
    }

    /**
     * Method that gets a Webpages connections, communicating with the barrels
     * @param URL URL
     * @return Webpage's connections
     * @throws RemoteException If a remote communication error occurs.
     */
    public String getConnections(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        String result = "Resultado: \n";
        System.out.println("[GATEWAY]: Getting connections for URL: " + URL);
        connectToBarrel();
        result = result.concat(barrel.getConnections(URL));
        if (result.isEmpty())
            return "No connections found";
        return result;
    }
    /**
     * Main method
     * @param args Queue IP and port, Gateway port, Barrel manager port, Whitelist path, Backup path
     * @throws RemoteException If a remote communication error occurs.
     */
    public static void main(String[] args) throws RemoteException{
        if(args.length != 6){
            System.out.println("Usage: java Gateway <queueAddress> <queuePort> <gatewayPort> <barrelManagerPort> <barrelWhitelistPath> <backupPath>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            String queueAddress = "rmi://" + args[0] + ":" + args[1] + "/queue";
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[2]));
            Gateway gateway = new Gateway( registry,
                    queueAddress, Integer.parseInt(args[3]), args[4], args[5]);
        }
        catch (RemoteException e) {
            System.out.println("Failed to initiliaze gateway");
        }
    }

}
