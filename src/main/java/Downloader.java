import java.awt.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;

public class Downloader implements Runnable {
    //Multicast section
    //TODO -> estes são os valores da ficha, verificar se são os corretos
    private final String MULTICAST_ADDRESS;
    private final int PORT;

    private final int CONFIRMATION_PORT;
    private final MulticastSocket socket;

    private final MulticastSocket confirmationSocket;
    QueueInterface queue;
    boolean queueExists = true;
    private final int downloaderNumber;
    String[] stopwords = {
            // English stopwords
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with",
            "about", "above", "after", "all", "also", "although", "always", "am", "anymore", "anyone", "anything",
            "anywhere", "because", "before", "being", "below", "between", "beyond", "can", "cannot", "could",
            "did", "do", "does", "doing", "done", "down", "during", "either", "else", "ever", "every", "everything",
            "everywhere", "few", "following", "from", "further", "had", "has", "have", "having", "here", "how", "i",
            "if", "into", "just", "least", "let", "like", "may", "me", "might", "more", "most", "much", "must",
            "neither", "never", "next", "nor", "now", "often", "once", "only", "other", "our", "ourselves", "out",
            "over", "own", "perhaps", "please", "same", "several", "should", "since", "so", "some", "still",
            "such", "than", "that", "then", "therefore", "these", "those", "through", "thus", "too", "under",
            "until", "up", "upon", "very", "via", "was", "we", "well", "were", "what", "whatever", "when",
            "where", "whereas", "whether", "which", "while", "who", "whom", "whose", "why", "will", "would",
            "yet", "you", "your", "yourselves",
            // Portuguese stopwords
            "a", "à", "ao", "aos", "às", "ante", "após", "até", "com", "contra", "de",
            "desde", "em", "entre", "para", "per", "perante", "por", "sem", "sob", "sobre",
            "trás", "o", "a", "os", "as", "um", "uma", "uns", "umas", "ao", "à", "às", "pelo",
            "pela", "pelos", "pelas", "do", "da", "dos", "das", "dum", "duma", "duns", "dumas",
            "no", "na", "nos", "nas", "num", "numa", "nuns", "numas", "pelo", "pela", "pelos",
            "pelas", "doutro", "doutra", "doutros", "doutras", "nel", "naquele", "naquela",
            "naqueles", "naquelas", "naqueloutro", "naqueloutra", "naqueloutros", "naqueloutras",
            "nela", "nele", "neles", "nelas", "neste", "neste", "nesta", "nestes", "nestas",
            "nisto", "nesse", "nessa", "nesses", "nessas",  "nisso"};

    HashSet<String> stopwordsSet;

    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());

    public Downloader(String multicastAddress, int port, String queuePath, int downloaderNumber) throws NotBoundException, IOException {
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.queue.clearQueue();
        this.downloaderNumber = downloaderNumber;
        this.socket = new MulticastSocket();
        this.stopwordsSet = new HashSet<>(Arrays.asList(stopwords));
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = port;
        this.CONFIRMATION_PORT = port + 1;
        this.confirmationSocket = new MulticastSocket(this.CONFIRMATION_PORT);
        // Join the multicast group
        InetAddress groupConfirmation = InetAddress.getByName(MULTICAST_ADDRESS);
        confirmationSocket.joinGroup(groupConfirmation);
        System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" + "   Ready...");
    }

    private void sendTokens(String hyperlink, Document doc) throws IOException {
        StringTokenizer tokens = new StringTokenizer(doc.text().replaceAll("[^a-zA-Z0-9\\s]", ""));
        String multicastMessage = "";
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken(); // Store the next token in a variable
            if(token.length()<3){
                continue;
            }
            if (multicastMessage.getBytes(StandardCharsets.UTF_8).length + token.getBytes(StandardCharsets.UTF_8).length+1 < 700) {
                if (!stopwordsSet.contains(token)) {
                    // Append the token to the multicast message
                    multicastMessage = multicastMessage.concat(" ").concat(token.toLowerCase());
                }
            } else {
                // Send the multicast message
                sendMulticastMessage(hyperlink, multicastMessage, MessageType.TOKENS);

                // Clear the multicast message
                multicastMessage = "";
            }
        }
    }

    private void updateURLs(String hyperlink, Document doc) throws IOException {
        String multicastMessage = "";
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String newURL = link.attr("abs:href");
            if (isValidURL(newURL)) {
                this.queue.addURL(newURL);
                //System.out.printf("[DOWNLOADER#" + downloaderNumber + "]:" +GETTING LINK" + "\t" + newURL + "\n");
                if (multicastMessage.getBytes(StandardCharsets.UTF_8).length + newURL.getBytes(StandardCharsets.UTF_8).length+1 < 700) {
                    multicastMessage = multicastMessage.concat(newURL.toLowerCase()).concat("^");
                }
                else {
                    //System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" + "Sending multicast message: " + multicastMessage);
                    sendMulticastMessage(hyperlink, multicastMessage, MessageType.CONNECTIONS);
                    multicastMessage = "";
                }
            }
        }
    }

    private boolean waitForConfirmation(String messageID){
        int messagesRead = 0;
        try{
            while(messagesRead < 500) {
                byte[] buffer = new byte[1500];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                confirmationSocket.receive(packet);
                MulticastMessage message = MulticastMessage.getMessage(packet.getData());
                messagesRead++;
                assert message != null;
                if(message.messageType() == MessageType.CONFIRMATION && message.payload().equals(messageID)){
                    return true;
            }

        }
            //System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" + "Confirmation not received, sending packet again.");
            return false;
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            return false;
        }
    }
    private void sendMulticastMessage(String hyperlink, String payload, MessageType messageType){
        // Check if the message is empty
        if (hyperlink == null || hyperlink.isEmpty() || hyperlink.isBlank()|| payload == null || payload.isEmpty() || payload.isBlank()) {
            System.out.println("Message is empty. Not sending anything.");
            return; // Exit the method if the message is empty
        }
        try{
            MulticastMessage message = new MulticastMessage(hyperlink, messageType, payload);

            byte[] buffer = message.getBytes();

            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            while (!waitForConfirmation(message.messageID())){
                socket.send(packet);
            }
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }

    }


    // TODO -> verificar se é necessário, se não for, apagar. (Pode ser substituído por um throw?)
    private boolean isValidURL(String url) {
        if (url == null || url.isEmpty() || url.isBlank()) {
            return false;
        }
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
    private void sendTitle(String hyperlink, Document doc){
        //verificar se há titulo
        String title;
        if(doc.title().isEmpty() || doc.title().isBlank()){
            title = "No title found.";
        }
        else{
            if(doc.title().getBytes(StandardCharsets.UTF_8).length + hyperlink.getBytes(StandardCharsets.UTF_8).length > 700) {
                // Calculate the remaining space for the text
                int remainingSpace = 700 - hyperlink.getBytes(StandardCharsets.UTF_8).length - 3;

                // Get the substring of the first paragraph text to fit the remaining space
                String truncatedText = new String(
                        doc.title().getBytes(StandardCharsets.UTF_8),
                        0,
                        remainingSpace,
                        StandardCharsets.UTF_8
                );
                // Append ellipsis to the truncated text
                title = truncatedText + "...";
            }
            else{
                title = doc.title();
            }
        }
        sendMulticastMessage(hyperlink, title, MessageType.TITLE);
    }

    private void sendCitation(String hyperlink, Document doc){
        //verificar se existe um firstParagraph
        Element firstParagraph = doc.select("p").first();
        String firstParagraphText;
        if(firstParagraph != null && !firstParagraph.text().isEmpty() && !firstParagraph.text().isBlank()){
            if(firstParagraph.text().getBytes(StandardCharsets.UTF_8).length + hyperlink.getBytes(StandardCharsets.UTF_8).length > 700) {
                // Calculate the remaining space for the text
                int remainingSpace = 700 - hyperlink.getBytes(StandardCharsets.UTF_8).length - 3;

                // Get the substring of the first paragraph text to fit the remaining space
                String truncatedText = new String(
                        firstParagraph.text().getBytes(StandardCharsets.UTF_8),
                        0,
                        remainingSpace,
                        StandardCharsets.UTF_8
                );
                // Append ellipsis to the truncated text
                firstParagraphText = truncatedText + "...";
            }
            else {
                firstParagraphText = firstParagraph.text();
            }
        }
        else {
            firstParagraphText = "No citation found.";
        }
        sendMulticastMessage(hyperlink, firstParagraphText, MessageType.CITATION);
    }

    public void run() {
        while (queueExists) {
            try {
                String url = this.queue.fetchURL();
                // System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" +"URL: " + url);
                Document doc = Jsoup.connect(url).get();
                sendTitle(url, doc);
                sendCitation(url, doc);
                sendTokens(url, doc);
                //atualizar a queue com os urls da página visitada e enviá-los por multicast
                updateURLs(url, doc);
            }
            catch (RemoteException remoteException) {
                // Set queueExists to false when RMI communication fails
                queueExists = false;
                socket.close();
            }
            catch (MalformedURLException malformedURLException){
                //System.out.println("[DOWNLOADER#" + downloaderNumber + "]: Malformed URL exception occurred, discarding hyperlink");
            }
            catch (SocketTimeoutException e){
                //System.out.println("[DOWNLOADER#" + downloaderNumber + "]: Socket timeout exception occurred, discarding hyperlink");
            }
            catch (HttpStatusException e) {
                //System.out.println("[DOWNLOADER#" + downloaderNumber + "]: HTTP status exception occurred, discarding hyperlink");
            }
            catch (IOException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }
        }

    }
    public static void main(String[] args) throws NotBoundException, IOException {
        Downloader downloader = new Downloader("224.3.2.1", 4321,"rmi://localhost/queue",1);
        downloader.run();
    }
    }






