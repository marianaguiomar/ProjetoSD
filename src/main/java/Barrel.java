import java.net.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Barrel extends UnicastRemoteObject implements BarrelInterface, Runnable{
    public static HashSet<Integer> activeBarrelIds = new HashSet<>();
    private final String MULTICAST_ADDRESS;
    private final int PORT;
    private final HashMap<String, HashSet<String>> index;
    private final HashMap<String, WebPage> webPages;
    private final HashMap<String, HashSet<String>> urlConnection;
    private final LinkedHashMap<String, Integer> searches;
    private final MulticastSocket socket;
    private final int barrelNumber;
    boolean multicastAvailable = true; // Initially assume multicast group is available

    public Barrel(Registry registry, String multicastAddress, int port, int barrelNumber) throws IOException {
        this.barrelNumber = barrelNumber;
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = port;
        this.socket = new MulticastSocket(PORT); // create socket and bind it
        this.index = new HashMap<>();
        this.webPages = new HashMap<>();
        this.urlConnection = new HashMap<>();
        this.searches = new LinkedHashMap<>();
        activeBarrelIds.add(barrelNumber);

        // Bind Barrel object to the existing registry
        try {
            registry.rebind("barrel" + barrelNumber, this);

        } catch (RemoteException e) {
            //String rmiAddress = "rmi://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT + "/barrel" + barrelNumber;
            //System.out.println("[BARREL#" + barrelNumber + "]:" + "    RMI Address: " + rmiAddress);
            e.printStackTrace();
        }
        System.out.println("[BARREL#" + barrelNumber + "]:" + "   Ready...");
    }

    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());

    private void addWebPage(String hyperlink, String titulo, String citacao){
        if(hyperlink.isBlank() || hyperlink.isEmpty() || titulo.isBlank() || titulo.isEmpty() || citacao.isBlank() || citacao.isEmpty()){
            return;
        }
        WebPage webPage = new WebPage(hyperlink, titulo, citacao);
        webPages.put(webPage.hyperlink(), webPage);
    }
    private void addIndex(String hyperlink, String token) {
        if (index.containsKey(token)) {
            index.get(token).add(hyperlink);
        }
        else {
            HashSet<String> urls = new HashSet<>();
            urls.add(hyperlink);
            index.put(token, urls);
        }
    }

    private void printHashMap() {

        for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
            String keyword = entry.getKey();
            HashSet<String> urls = entry.getValue();

            System.out.println("[BARREL#" + barrelNumber + "]:" + "    Keyword: " + keyword);
            System.out.println("[BARREL#" + barrelNumber + "]:" + "    URLs:");
            for (String url : urls) {
                System.out.println("[BARREL#" + barrelNumber + "]:" + "      " + url);
            }
        }
    }

    private String receiveMessage() throws IOException {
        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

    private String getHyperlink(String message) {
        // Find the index of the first '|' character
        int index = message.indexOf('|');

        // If '|' is not found, return the entire message as the title
        if (index == -1) {
            return "";
        }

        // Extract the title from the beginning of the message up to '|' (excluding '|')
        return message.substring(0, index);
    }

    private String getTitle(String message) {
        // Find the index of the first '|' character
        int index = message.indexOf('|');

        // If '|' is not found, return an empty string as there is no URL
        if (index == -1) {
            return "";
        }

        // Extract the URL from the message starting from the character after '|' (excluding '|')
        return message.substring(index + 1);
    }

    private String[] getTokens(String message) {
        return message.split(" ");
    }

    public WebPage[] search(String[] tokens, Integer pageNumber) throws RemoteException{
        LinkedList<WebPage> result = new LinkedList<>();
        for (String token: tokens) {
            if (index.containsKey(token)) {
                HashSet<String> URLs = index.get(token);
                Stream<WebPage> resultingWebPages = URLs.stream().filter(webPages::containsKey).map(webPages::get);
                resultingWebPages.forEach(result::add);
            }
        }
        String currKey = String.join(" ", tokens);
        if (searches.containsKey(currKey)) {
            System.out.println("[BARREL#" + barrelNumber + "]:" + "    Found! ");
            int curr = searches.get(currKey);
            searches.put(currKey, curr+1);
        }
        else {
            searches.put(currKey, 1);
        }
        orderWebpages(result);
        updateSearches();

        //append barrelID

        return result.subList(pageNumber, Math.min(pageNumber + 10, result.size())).toArray(new WebPage[0]);
    }

    public void orderWebpages(LinkedList<WebPage> result) {
        // Define a custom comparator based on the length of the attribute in descending order
        Comparator<WebPage> comparator = new Comparator<WebPage>() {
            @Override
            public int compare(WebPage page1, WebPage page2) {
                String url1 = page1.hyperlink();
                String url2 = page2.hyperlink();

                int connections1 = urlConnection.get(url1).size();
                int connections2 = urlConnection.get(url2).size();

                // Compare the lengths of the attribute in each WebPage in reverse order
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
        String resultString = result.toString();

        return resultString;
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
        Collections.sort(entryList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

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
        String result = "";

        //TODO -> verificar casos a falhar, "https://nytimes.com"
        if (urlConnection.containsKey(URL)) {
            for (String url: urlConnection.get(URL)) {
                result = result.concat(url).concat("\n");
            }
        }
        //TODO -> alterar isto
        else result = "Link inválido \n";

        return result;
    }

    private void addURLConnections(String url, String hyperlink) {
        HashSet<String> currentResult;
        if (urlConnection.containsKey(url)) {
            currentResult = urlConnection.get(url);
            currentResult.add(hyperlink);
        }
        else {
            currentResult = new HashSet<>();
            currentResult.add(hyperlink);
            urlConnection.put(url, currentResult);
        }
    }

    private void receiveTokens(String hyperlink) throws IOException {

        String message = receiveMessage();
        while (message.charAt(0) != '\u0003') {
            String[] tokens = getTokens(message);
            for (String token: tokens) {
                addIndex(hyperlink, token);
            }
            message = receiveMessage();
        }
    }

    private void receiveConnections(String hyperlink) throws IOException {
        if (!urlConnection.containsKey(hyperlink)) {
            urlConnection.put(hyperlink, new HashSet<>());
        }
        String message = receiveMessage();
        while (message.charAt(0) != '§') {
            String[] mentionsURLS = getTokens(message);
            for (String url: mentionsURLS) {
                addURLConnections(url, hyperlink);
            }
            message = receiveMessage();
        }

    }


    public void run() {
        try {
            InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(new InetSocketAddress(mcastaddr, 0), NetworkInterface.getByIndex(0));

            while (multicastAvailable) {
                String message = receiveMessage();
                String titulo = getTitle(message);
                String hyperlink = getHyperlink(message);
                String citacao = receiveMessage();
                System.out.println("Adding citation: " + citacao + "\n");
                addWebPage(hyperlink, titulo, citacao);
                receiveTokens(hyperlink);
                receiveConnections(hyperlink);
                //printHashMap();

            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);

        } finally {
            // Perform cleanup operations
            try {
                socket.leaveGroup(new InetSocketAddress(this.MULTICAST_ADDRESS, this.PORT), NetworkInterface.getByIndex(0));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while leaving multicast group: " + e.getMessage(), e);
            }
            socket.close();
            multicastAvailable = false;
        }
    }
    public static void main(String[] args) throws IOException {
        try {
            // Create RMI registry
            Registry registry = LocateRegistry.createRegistry(4321);
            Barrel barrel = new Barrel(registry, "224.3.2.1", 4321, 0);
            barrel.run();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}