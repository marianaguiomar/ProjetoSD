import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.LinkedList;

public class Downloader {
    //TODO -> ESTAR SEMPRE A RECEBER URL DA QUEUE
    //TODO -> ver o que é mais indicado usar. queue? deque? por enquanto fica linked list (que é uma forma de queue?)
    //TODO -> ver se deixa a queue aqui, num ficheiro à parte, dentro de outras cenas, etc


    LinkedList<String> URLQueue = new LinkedList<>();

    public void addToQueue() {
        URLQueue.add("https://sapo.pt");
        //URLQueue.add("https://observador.pt");
        //URLQueue.add("https://inforestudante.uc.pt");
    }



    public static void main(String args[]) {

        Downloader downloader = new Downloader();

        downloader.URLQueue.clear();

        downloader.addToQueue();

        //String url = args[0];

        for (String url2 : downloader.URLQueue) {
            try {
                System.out.println("\n\n\n SITE NOVO \n\n\n");
                Document doc = Jsoup.connect(url2).get();
                StringTokenizer tokens = new StringTokenizer(doc.text());
                int countTokens = 0;

                // exemplo -> este imprime os primeiros 100 tokens em lowercase
                while (tokens.hasMoreElements() && countTokens++ < 100)
                    System.out.println(tokens.nextToken().toLowerCase());

                // seleciona todos os elementos <a> que têm um atributo href -> tipicamente são hiperlinks
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



