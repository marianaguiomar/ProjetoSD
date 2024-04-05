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
    private final HashMap<Integer,Long> totalDuration;

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
     * @throws RemoteException If a remote communication error occurs.
     */
    public Gateway(Registry registry, String queuePath, int barrelManagerPort) throws RemoteException {
        super();
        this.barrelInUse = 0;
        this.queuePath = queuePath;
        this.totalDuration = new HashMap<>();
        this.numSearches = new HashMap<>();
        this.searches = new LinkedHashMap<>();
        this.barrelManager = new BarrelManager(barrelManagerPort,"./src/main/Project/Googol/Manager/BarrelManager/whitelist.txt");
        registry.rebind("gateway", this);
    }

    /**
     * Method that connects the Gateway to the Queue
     */
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

    /**
     * Method that connects the Gateway to a barrel
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
     * Method that performs Gateway's operations while it's running
     */
    public void run(){
        try {
            connectToQueue();
            connectToBarrel();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[GATEWAY]: Gateway Ready...");
    }

    /**
     * Method that updates searches performed (by each barrel and in total)
     * @param duration Time it took to perform a search
     * @throws RemoteException If a remote communication error occurs.
     */
    public void updateSearches(long duration) throws RemoteException{
        if (!totalDuration.containsKey(this.barrel.getBarrelNumber())) {
            totalDuration.put(this.barrel.getBarrelNumber(), duration);
        }
        else {
            long currDuration = totalDuration.get(this.barrel.getBarrelNumber());
            long updatedDuration = currDuration + duration;
            totalDuration.put(this.barrel.getBarrelNumber(), updatedDuration);
        }

        if (!numSearches.containsKey(this.barrel.getBarrelNumber())) {
            numSearches.put(this.barrel.getBarrelNumber(), 1);
        }
        else {
            long currSearches = numSearches.get(this.barrel.getBarrelNumber());
            long updatedSearches = currSearches + 1;
            totalDuration.put(this.barrel.getBarrelNumber(), updatedSearches);
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
        WebPage[] webPages = null;
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
            long averageDuration = totalDuration.get(barrel) / numSearches.get(barrel);
            result.append("BARREL#").append(barrel).append(": ").append(averageDuration).append(" ms.\n");
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
        result = result.concat(barrel.getConnections(URL));
        return result;
    }

    public static void main(String[] args) throws RemoteException{
        if(args.length != 4){
            System.out.println("Usage: java Gateway <queueAddress> <queuePort> <gatewayPort> <barrelManagerPort>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            String queueAddress = "rmi://" + args[0] + ":" + args[1] + "/queue";
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[2]));
            Gateway gateway = new Gateway( registry,
                    queueAddress, Integer.parseInt(args[3]));
            gateway.run();
        }
        catch (RemoteException e) {
            System.out.println("Failed to initiliaze gateway");
        }
    }

}
