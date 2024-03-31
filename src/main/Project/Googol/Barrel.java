package Googol;

import Multicast.MulticastMessage;
import Multicast.Receiver;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Barrel extends UnicastRemoteObject implements BarrelInterface, Runnable{
    // TODO -> see static variable
    public static HashSet<Integer> activeBarrelIds = new HashSet<>();
    private final Receiver receiver;
    private final RemissiveIndex remissiveIndex;
    private final LinkedHashMap<String, Integer> searches;
    private final int barrelNumber;
    boolean multicastAvailable = true; // Initially assume multicast group is available

    public Barrel(Registry registry, String multicastAddress, int port, String confirmationMulticastAddress, int confirmationPort, int barrelNumber) throws IOException {
        this.barrelNumber = barrelNumber;
        this.searches = new LinkedHashMap<>();
        this.remissiveIndex = new RemissiveIndex();
        activeBarrelIds.add(barrelNumber);
        this.receiver = new Receiver(multicastAddress, port, confirmationPort);
        // Bind Googol.Barrel object to the existing registry
        try {
            registry.rebind("barrel" + barrelNumber, this);

        } catch (RemoteException e) {
            //String rmiAddress = "rmi://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT + "/barrel" + barrelNumber;
            //System.out.println("[BARREL#" + barrelNumber + "]:" + "    RMI Address: " + rmiAddress);
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
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

    public WebPage[] searchUnion(String[] tokens, Integer pageNumber) throws RemoteException{
        LinkedList<WebPage> result = remissiveIndex.findWebPagesUnion(tokens);
        if(result == null || result.isEmpty())
            return new WebPage[0];
        String currKey = String.join(" ", tokens).toLowerCase();
        if (searches.containsKey(currKey)) {
            int curr = searches.get(currKey);
            searches.put(currKey, curr+1);
        }
        else {
            searches.put(currKey, 1);
        }
        orderWebpages(result);
        updateSearches();

        if(result.size() < pageNumber * 10)
            return new WebPage[0];
        return result.subList(pageNumber * 10, Math.min(pageNumber * 10 + 10, result.size())).toArray(new WebPage[0]);
    }

    public WebPage[] searchIntersection(String[] tokens, Integer pageNumber) throws RemoteException{
        LinkedList<WebPage> result = remissiveIndex.findWebPagesIntersection(tokens);
        if(result == null || result.isEmpty())
            return new WebPage[0];
        String currKey = String.join(" ", tokens).toLowerCase();
        if (searches.containsKey(currKey)) {
            int curr = searches.get(currKey);
            searches.put(currKey, curr+1);
        }
        else {
            searches.put(currKey, 1);
        }
        orderWebpages(result);
        updateSearches();

        if(result.size() < pageNumber * 10)
            return new WebPage[0];
        return result.subList(pageNumber * 10, Math.min(pageNumber * 10 + 10, result.size())).toArray(new WebPage[0]);
    }

    public void orderWebpages(LinkedList<WebPage> result) {
        // Define a custom comparator based on the length of the attribute in descending order
        Comparator<WebPage> comparator = new Comparator<WebPage>() {
            @Override
            public int compare(WebPage page1, WebPage page2) {
                String url1 = page1.getHyperlink();
                String url2 = page2.getHyperlink();

                int connections1 = remissiveIndex.getNumberOfConnections(url1);
                int connections2 = remissiveIndex.getNumberOfConnections(url2);

                // Compare the lengths of the attribute in each Googol.WebPage in reverse order
                return Integer.compare(connections2, connections1); // Compare in reverse order
            }
        };

        // Sort the result list using the custom comparator
        Collections.sort(result, comparator);
    }

    public String status() throws RemoteException {
        String topTen = formatSearches();
        String barrelID = formatActiveBarrels();

        String res = "" ;
        res = res.concat(topTen);
        res = res.concat("§");
        res = res.concat(barrelID);
        return res;
    }

    public String formatActiveBarrels() {
        // Create a StringBuilder to build the result string
        StringBuilder result = new StringBuilder();

        result.append("\nACTIVE BARRELS\n");

        // Append each value with the specified format to the StringBuilder
        for (int value : activeBarrelIds) {
            result.append("BARREL#").append(value).append("\n");
        }

        // Convert the StringBuilder to a string

        return result.toString();
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

    private void printSearches() {
        for (Map.Entry<String, Integer> entry : searches.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("[BARREL#" + barrelNumber + "]:" + "    \n");
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);

        } finally {
            // Perform cleanup operations
            /*
            try {
                socket.leaveGroup(new InetSocketAddress(this.MULTICAST_ADDRESS, this.PORT), NetworkInterface.getByIndex(0));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while leaving multicast group: " + e.getMessage(), e);
            }
            */
            multicastAvailable = false;
        }
    }
    public static void main(String[] args) throws IOException {
        try {
            // Create RMI registry
            Registry registry = LocateRegistry.createRegistry(4321);
            Barrel barrel = new Barrel(registry, "224.3.2.1", 4321, "224.3.2.2", 4322, 0);
            barrel.run();
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }
    }
}