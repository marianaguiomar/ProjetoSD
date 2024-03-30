import java.net.*;
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
    private final MulticastSocket socket;
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
        System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" + "   Ready...");
    }

    private void sendToken(StringTokenizer tokens) throws IOException {
        String multicastMessage = "";

        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken(); // Store the next token in a variable

            if (multicastMessage.length() + token.length()+1 < 700) {
                if (!stopwordsSet.contains(token)) {
                    // Append the token to the multicast message
                    multicastMessage = multicastMessage.concat(" ").concat(token.toLowerCase());
                }
            } else {
                // Send the multicast message
                sendMulticastMessage(multicastMessage);

                // Clear the multicast message
                multicastMessage = "";
            }
        }
    }

    private void updateURLs(Document doc) throws IOException {

        String multicastMessage = "";

        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String newURL = link.attr("abs:href");
            if (isValidURL(newURL)) {
                this.queue.addURL(newURL);
                //System.out.printf("[DOWNLOADER#" + downloaderNumber + "]:" +GETTING LINK" + "\t" + newURL + "\n");
                if (multicastMessage.length() + newURL.length()+1 < 700) {
                    multicastMessage = multicastMessage.concat(" ").concat(newURL.toLowerCase());
                }
                else {
                    sendMulticastMessage(multicastMessage);
                    multicastMessage = "";
                }
            }
        }
    }


    private void sendMulticastMessage(String message) throws IOException {
        // Check if the message is empty
        if (message == null || message.isEmpty()) {
            //System.out.println("Message is empty. Not sending anything.");
            return; // Exit the method if the message is empty
        }

        byte[] buffer = message.getBytes();

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        socket.send(packet);
    }


    // TODO -> verificar se é necessário, se não for, apagar. (Pode ser substituído por um throw?)
    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    public void run() {
        while (queueExists) {
            try {
                String url = this.queue.fetchURL();
                System.out.println("[DOWNLOADER#" + downloaderNumber + "]:" +"URL: " + url);
                Document doc = Jsoup.connect(url).get();
                //verificar se existe um firstParagraph
                Element firstParagraph = doc.select("p").first();
                if(firstParagraph == null){
                    continue;
                }
                String firstParagraphText = firstParagraph.text();
                // enviar url e título
                sendMulticastMessage(url + "|" + doc.title());
                //enviar citação
                sendMulticastMessage(firstParagraphText);
                //enviar os tokens
                StringTokenizer tokens = new StringTokenizer(doc.text());
                sendToken(tokens);
                //mandar divisor entre tokens e urls
                sendMulticastMessage("\u0003");
                //atualizar a queue com os urls da página visitada e enviá-los
                updateURLs(doc);
                //enviar mensagem final
                sendMulticastMessage("§");
            }
            catch (RemoteException remoteException) {
                // Set queueExists to false when RMI communication fails
                queueExists = false;
                socket.close();
            }
            catch (HttpStatusException e) {
                // do nothing
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






