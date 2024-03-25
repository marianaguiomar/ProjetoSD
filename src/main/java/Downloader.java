import java.net.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.StringTokenizer;

//TODO -> EXCEPTION MALFORMED URL (JSOUP) PARA URLS INVÁLIDOS
public class Downloader {
    //Multicast section
    //TODO -> estes são os valores da ficha, verificar se são os corretos
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;
    //private long SLEEP_TIME = 5000;
    private String channel;

    private MulticastSocket socket = null;
    QueueInterface queue;

    //TODO -> mais stopwords
    String[] stopwords = {"a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"};

    public Downloader(String queuePath) throws MalformedURLException, RemoteException, NotBoundException, IOException {
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.queue.clearQueue();
        this.socket = new MulticastSocket();
    }

    public void sendToken(StringTokenizer tokens) throws UnknownHostException, IOException {
        String multicastMessage = "";

        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken(); // Store the next token in a variable

            if (multicastMessage.length() + token.length()+1 < 700) {
                boolean isStopword;
                if (Arrays.asList(stopwords).contains(token)) isStopword = true;
                else isStopword = false;

                if (!isStopword) {
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


    public void sendMulticastMessage(String message) throws UnknownHostException, IOException{
        byte[] buffer = message.getBytes();

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        socket.send(packet);
    }

    public void work()  throws MalformedURLException, RemoteException, NotBoundException, SocketTimeoutException {
        this.queue.clearQueue();
        this.queue.addURL("https://www.sapo.pt");

        while (true) {
            try {

                socket = new MulticastSocket();  // create socket without binding it (only for sending)

                String url = this.queue.fetchURL();
                Document doc = Jsoup.connect(url).get();

                try {
                    //verificar se existe um firstParagraph
                    Element firstParagraph = doc.select("p").first();
                    String firstParagraphText = firstParagraph.text();

                    // enviar url e título
                    sendMulticastMessage(url + "|" + doc.title());

                    //enviar citação
                    sendMulticastMessage(firstParagraphText);
                } catch (java.lang.NullPointerException e) {
                    continue;
                }

                //enviar os tokens
                StringTokenizer tokens = new StringTokenizer(doc.text());
                sendToken(tokens);

                sendMulticastMessage("§");


                // seleciona todos os elementos <a> que têm um atributo href -> tipicamente são hiperlinks
                // TODO -> estes hiperlinks não serão enviados por Multicast, vão ser inseridos na URLQueue
                Elements links = doc.select("a[href]");
                for (Element link : links)
                    // imprime o texto (o que está visível e clicável) e o url absoluto de cada link
                    //System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                    this.queue.addURL(link.attr("abs:href"));
                    System.out.println("GETTING LINK" + "\t" + this.queue.fetchURL() + "\n");

                    /*
                    String message = this.queue.fetchURL();
                    byte[] buffer = message.getBytes();

                    InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                    socket.send(packet);
                    */





            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }
        }

    }
    public static void main(String args[]) throws MalformedURLException, RemoteException, NotBoundException, IOException {

        Downloader downloader = new Downloader("rmi://localhost/queue");

        downloader.work();


    }

    }






