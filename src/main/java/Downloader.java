import java.net.MalformedURLException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

// TODO -> EXCEPTION MALFORMED URL (JSOUP) PARA URLS INVÁLIDOS
public class Downloader {
    //Multicast section
    //TODO -> estes são os valores da ficha, verificar se são os corretos
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;
    //private long SLEEP_TIME = 5000;
    private String channel;
    QueueInterface queue;

    public Downloader(String queuePath) throws MalformedURLException, RemoteException, NotBoundException {
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.queue.clearQueue();
    }

    public void work()  throws MalformedURLException, RemoteException, NotBoundException{
        this.queue.clearQueue();
        this.queue.addURL("https://www.sapo.pt");

        while (true) {
            try {
                String url2 = this.queue.fetchURL();
                Document doc = Jsoup.connect(url2).get();
                StringTokenizer tokens = new StringTokenizer(doc.text());
                int countTokens = 0;

                // exemplo -> este imprime os primeiros 100 tokens em lowercase
                // TODO -> acho que têm de ser todos os tokens que existem?
                // TODO -> limitar os tokens para não conterem palavras reservadas
                //while (tokens.hasMoreElements() && countTokens++ < 100)
                    //System.out.println(tokens.nextToken().toLowerCase());

                // seleciona todos os elementos <a> que têm um atributo href -> tipicamente são hiperlinks
                // TODO -> estes hiperlinks não serão enviados por Multicast, vão ser inseridos na URLQueue
                Elements links = doc.select("a[href]");
                for (Element link : links)
                    // imprime o texto (o que está visível e clicável) e o url absoluto de cada link
                    //System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
                    this.queue.addURL(link.attr("abs:href"));
                    System.out.println("GETTING LINK" + "\t" + this.queue.fetchURL() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public static void main(String args[]) throws MalformedURLException, RemoteException, NotBoundException {

        Downloader downloader = new Downloader("rmi://localhost/queue");

        downloader.work();


    }

    }






