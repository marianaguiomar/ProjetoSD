package Googol;
import Googol.Queue.QueueInterface;
import Multicast.MessageType;
import Multicast.Sender;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that manages a Downloader
 */
public class Downloader implements Runnable {
    /**
     * URL Queue
     */
    QueueInterface queue;

    /**
     * Sender (sends MulticastMessages)
     */
    private final Sender sender;

    /**
     * Boolean that returns wether a queue exists or not
     */
    boolean queueExists = true;

    /**
     * Port
     */
    private final int port;
    //TODO -> ??
    /**
     * ID
     */
    private final Integer myID;

    /**
     * Set of URL that have been visited
     */
    private final HashSet<String> visitedURL;

    /**
     * Stopwords that won't be sent as tokens to the barrels
     */
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

    //todo -> ???
    HashSet<String> stopwordsSet;

    /**
     * Logger to print error messages
     */
    private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());


    /**
     * Class constructor, attributes are initialized
     * @param multicastAddress Multicast address
     * @param port Port
     * @param confirmationPort Port to receive ACKs
     * @param queuePath Path to queue
     * @param ID Downloader ID
     * @throws NotBoundException Remote object is not bound to the specified name in the registry.
     * @throws IOException An I/O exception has occurred.
     */
    public Downloader(String multicastAddress, int port, int confirmationPort,
                      String queuePath, int ID) throws NotBoundException, IOException {
        this.port = port;
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.myID =  ID;
        if(!this.queue.verifyID(this.myID,getMyAddress(), port)){
            System.out.println("[BARREL#" + this.myID + "]:" + "   Downloader ID is not valid. Exiting...");
            System.exit(1);
        }
        this.sender = new Sender(multicastAddress, port,  confirmationPort);
        this.stopwordsSet = new HashSet<>(Arrays.asList(stopwords));
        this.visitedURL = new HashSet<>();
        System.out.println("[DOWNLOADER#" + myID + "]:" + "   Ready...");
        Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
    }

    /**
     * Method that has queue remove this downloader from the list of active downloaders when it stops running
     */
    private void exit() {
        try {
            this.queue.removeInstance(getMyAddress(), this.port,this.myID);
        }
        catch(RemoteException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method that returns localhost address
     * @return localhost address
     * @throws UnknownHostException Hostname provided is unknown or could not be resolved.
     */
    private String getMyAddress() throws UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();
        return address.getHostAddress();
    }

    /**
     * Method that send MulticastMessages with tokens
     * @param hyperlink hyperlink where the tokens where found
     * @param doc website, from connection to sjoup
     * @throws IOException An I/O exception has occurred.
     */
    private void sendTokens(String hyperlink, Document doc) throws IOException {
        StringTokenizer tokens = new StringTokenizer(doc.text());
        //System.out.println(tokens);
        String multicastMessage = "";
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken().replaceAll("[^a-zA-Z0-9\\-\\s]", "").trim(); // Store the next token in a variable
            if(token.length()<3){
                continue;
            }
            if (multicastMessage.getBytes(StandardCharsets.UTF_8).length + token.getBytes(StandardCharsets.UTF_8).length+1 < 700 && tokens.hasMoreTokens()) {
                if (!stopwordsSet.contains(token)) {
                    // Append the token to the multicast message
                    multicastMessage = multicastMessage.concat(" ").concat(token.toLowerCase());
                }
            } else {
                // Send the multicast message
                sender.sendMessage(hyperlink, multicastMessage, MessageType.TOKENS);

                // Clear the multicast message
                multicastMessage = "";
            }
        }
    }

    /**
     * Method that sends MulticastMessages with all URLs referenced in the hyperlink
     * @param hyperlink hyperlink
     * @param doc website, from connection to sjoup
     * @throws IOException An I/O exception has occurred.
     */
    private void updateURLs(String hyperlink, Document doc) throws IOException {
        String multicastMessage = "";
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String newURL = link.attr("abs:href");
            if (isValidURL(newURL)) {
                visitedURL.add(newURL);
                this.queue.addURL(newURL);
                //System.out.println("[DOWNLOADER#" + myID + "]:" + "INSERTING LINK" + "\t" + newURL); // Change here
                if (multicastMessage.getBytes(StandardCharsets.UTF_8).length + newURL.getBytes(StandardCharsets.UTF_8).length+1 < 700) {
                    multicastMessage = multicastMessage.concat(newURL.toLowerCase()).concat("^");
                }
                else {
                    //System.out.println("[DOWNLOADER#" + myID + "]:" + "Sending multicast message: " + multicastMessage);
                    sender.sendMessage(hyperlink, multicastMessage, MessageType.CONNECTIONS);
                    multicastMessage = "";
                }
            }
        }
    }

    /**
     * Method that evaluates if a given URL is valid
     * @param url URL
     * @return True if the URL is valid
     */
    private boolean isValidURL(String url) {
        if (url == null || url.isEmpty() || url.isBlank() || visitedURL.contains(url)) {
            return false;
        }
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    /**
     * Method that sends a MulticastMessage with the title
     * @param hyperlink hyperlink
     * @param doc website, from connection to sjoup
     */
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
        //System.out.println("[DOWNLOADER#" + myID + "]:" + "Title: " + title);
        sender.sendMessage(hyperlink, title, MessageType.TITLE);
    }

    /**
     * Method that sends a MulticastMessage with the citation
     * @param hyperlink hyperlink
     * @param doc website, from connection to sjoup
     */
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
        sender.sendMessage(hyperlink, firstParagraphText, MessageType.CITATION);
    }

    /**
     * Method that performs Downloader's operations while it's running
     */
    public void run() {
        while (queueExists) {
            try {
                String url = this.queue.fetchURL();
                System.out.println("[DOWNLOADER#" + myID + "]:" +"URL: " + url);
                Document doc;
                try {
                    doc = Jsoup.connect(url).get();
                } catch(Exception e){
                    //System.out.println("[DOWNLOADER#" + myID + "]: HTTP status exception occurred, discarding hyperlink");
                    continue;
                }
                System.out.println(doc.text());
                sendTitle(url, doc);
                sendCitation(url, doc);
                sendTokens(url, doc);
                //atualizar a queue com os urls da página visitada e enviá-los por multicast
                updateURLs(url, doc);
            }
            catch (RemoteException remoteException) {
                // Set queueExists to false when RMI communication fails
                queueExists = false;
            }
            catch (MalformedURLException malformedURLException){
                //System.out.println("[DOWNLOADER#" + myID + "]: Malformed URL exception occurred, discarding hyperlink");
            }
            catch (SocketTimeoutException e){
                //System.out.println("[DOWNLOADER#" + myID + "]: Socket timeout exception occurred, discarding hyperlink");
            }
            catch (HttpStatusException e) {
                //System.out.println("[DOWNLOADER#" + myID + "]: HTTP status exception occurred, discarding hyperlink");
            }
            catch (IOException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }
        }

    }
    public static void main(String[] args) throws NotBoundException, IOException {
        if(args.length != 6){
            System.out.println("Usage: java Downloader <multicastAddress> <port> <confirmationPort> <queueIP> <queuePort> <ID>");
            System.exit(1);
        }
        String queueAddress = "rmi://" + args[3] + ":" + args[4] + "/queue";
        Downloader downloader = new Downloader(args[0], Integer.parseInt(args[1]),
                Integer.parseInt(args[2]), queueAddress, Integer.parseInt(args[5]));
        downloader.run();
    }
    }






