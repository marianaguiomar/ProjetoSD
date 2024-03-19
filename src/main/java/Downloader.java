import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Downloader extends Thread {
    //TODO -> ficou concurrent double deque
    //TODO -> ver se deixa a queue aqui, num ficheiro à parte, dentro de outras cenas, etc

    //Multicast section
    //TODO -> estes são os valores da ficha, verificar se são os corretos
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;
    //private long SLEEP_TIME = 5000;

    // TODO -> nome da thread, mudar potencialmente para Downloader <número do downloader>

    private String channel;


    public Downloader() {
        //TODO -> nome (por um argumento no main? para distinguir os downloaders) por id downloader
        super("Downloader " + (long) (Math.random() * 1000));
        this.channel = channel;
    }

    public void run() {
        MulticastSocket socket = null;
        long counter = 0;
        System.out.println(this.getName() + " running...");
        try {
            socket = new MulticastSocket();  // create socket without binding it (only for sending)
            while (true) {
                //TODO -> esta parte será o que enviará as mensagens (de momento só tem o código da ficha 3)
                String message = this.getName() + " packet " + counter++;
                byte[] buffer = message.getBytes();

                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);

                // vai enviar o current link a ser analisado e os tokens associados. os links encontrados são postos na fila para serem analisados
                // TODO -> ver se se dá para enviar objetos por multicast, se sim por aqui a classe URL
                socket.send(packet);

                try {
                    //TODO -> ver do sleep, ver se é necessário (não é, era só para o exercício)
                    sleep((long) (Math.random()));
                } catch (InterruptedException e) { }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }




    LinkedList<String> URLQueue = new LinkedList<>();

    // TODO -> ordem de adicionar os links
    public void addToQueue() {
        // TODO -> vai começar já com sites? (por enquanto é melhor deixar um i guess)
        URLQueue.add("https://sapo.pt");
        //URLQueue.add("https://observador.pt");
        //URLQueue.add("https://inforestudante.uc.pt");
    }



    public static void main(String args[]) {

        Downloader downloader = new Downloader();
        downloader.start();

        // TODO -> verificar isto
        downloader.URLQueue.clear();

        downloader.addToQueue();

        //String url = args[0];

        //TODO -> ESTAR SEMPRE A RECEBER URL DA QUEUE (while queue isn't empty)

        for (String url2 : downloader.URLQueue) {
            try {
                System.out.println("\n\n\n SITE NOVO \n\n\n");
                Document doc = Jsoup.connect(url2).get();
                StringTokenizer tokens = new StringTokenizer(doc.text());
                int countTokens = 0;

                // exemplo -> este imprime os primeiros 100 tokens em lowercase
                // TODO -> acho que têm de ser todos os tokens que existem?
                // TODO -> limitar os tokens para não conterem palavras reservadas
                while (tokens.hasMoreElements() && countTokens++ < 100)
                    System.out.println(tokens.nextToken().toLowerCase());

                // seleciona todos os elementos <a> que têm um atributo href -> tipicamente são hiperlinks
                // TODO -> estes hiperlinks não serão enviados por Multicast, vão ser inseridos na URLQueue
                Elements links = doc.select("a[href]");
                for (Element link : links)
                    // imprime o texto (o que está visível e clicável) e o url absoluto de cada link
                    System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}



