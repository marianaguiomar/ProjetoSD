import java.net.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Barrel extends UnicastRemoteObject implements BarrelInterface{
    private final String MULTICAST_ADDRESS;
    private final int PORT;
    private final HashMap<String, HashSet<WebPage>> index;

    private final MulticastSocket socket;
    boolean multicastAvailable = true; // Initially assume multicast group is available

    public Barrel() throws IOException {
        this.MULTICAST_ADDRESS = "224.3.2.1";
        this.PORT = 4321;
        this.socket = new MulticastSocket(PORT); // create socket and bind it
        this.index = new HashMap<>();
    }

    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());

    private void addToIndex(WebPage webPage, String token) {
        if (index.containsKey(token)) {
            if(index.get(token).contains(webPage)){
                return;
            }
            index.get(token).add(webPage);
        }
        else {
            HashSet<WebPage> webPages = new HashSet<>();
            webPages.add(webPage);
            index.put(token, webPages);
        }
    }

    private void printHashMap() {

        for (Map.Entry<String, HashSet<WebPage>> entry : index.entrySet()) {
            String keyword = entry.getKey();
            HashSet<WebPage> urls = entry.getValue();

            System.out.println("Keyword: " + keyword);
            System.out.println("URLs:");
            for (WebPage url : urls) {
                System.out.println("  " + url);
            }
        }
    }

    private String receiveMessage() throws IOException {
        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

    private String getURL(String message) {
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
        LinkedList<WebPage> webPages = new LinkedList<>();
        for (String token: tokens) {
            if (index.containsKey(token)) {
                webPages.addAll(index.get(token));
            }
        }
        return webPages.subList(pageNumber, Math.min(pageNumber + 10, webPages.size())).toArray(new WebPage[0]);
    }
    public String status() throws RemoteException {
        return "Barrel is running";
    }
    public String getConnections(String URL) throws RemoteException{
        return "webPages.toString()";
    }
    public void work() {
    try {
        InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
        socket.joinGroup(new InetSocketAddress(mcastaddr, 0), NetworkInterface.getByIndex(0));

        while (multicastAvailable) {

            String message = receiveMessage();
            String titulo = getTitle(message);
            String url = getURL(message);

            message = receiveMessage();
            String citacao = message;

            WebPage webPage = new WebPage(url, titulo, citacao);

            message = receiveMessage();
            while (message.charAt(0) != '§') {
                String[] tokens = getTokens(message);
                for (String token: tokens) {
                    addToIndex(webPage, token);
                }
                message = receiveMessage();
            }

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
        Barrel barrel = new Barrel();
        LocateRegistry.createRegistry(barrel.PORT).rebind("barrel", barrel);
        barrel.work();
    }
}
