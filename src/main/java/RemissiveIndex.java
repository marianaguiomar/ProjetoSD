import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

public class RemissiveIndex {
    public final HashMap<String, HashSet<String>> index;
    public final HashMap<String, WebPage> webPages;
    public final HashMap<String, HashSet<String>> urlConnection;

    public void addWebPage(String hyperlink, String titulo, String citacao){
        WebPage webPage = new WebPage(hyperlink, titulo, citacao);
        webPages.put(webPage.hyperlink(), webPage);
    }

    public String getConnections(String hyperlink){
        String header = "CONNECTIONS TO LINK: " + hyperlink + "\n";
        StringBuilder result = new StringBuilder(header);
        if (urlConnection.containsKey(hyperlink)) {
            for (String url: urlConnection.get(hyperlink)) {
                if(webPages.containsKey(url))
                    result.append(webPages.get(url).toString());
            }
        }
        return result.toString();
    }
    public LinkedList<WebPage> findWebPages(String[] tokens){
        LinkedList<WebPage> result = new LinkedList<>();
        for (String token: tokens) {
            if (index.containsKey(token)) {
                HashSet<String> URLs = index.get(token);
                Stream<WebPage> resultingWebPages = URLs.stream().filter(webPages::containsKey).map(webPages::get);
                resultingWebPages.forEach(result::add);
            }
        }
        return result;
    }

    public int getNumberOfConnections(String hyperlink){
        if(!urlConnection.containsKey(hyperlink)){
            return 0;
        }
        return urlConnection.get(hyperlink).size();
    }

    public void printIndexHashMap(int barrelNumber) {

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
    public void addIndex(String hyperlink, String token) {
        if (index.containsKey(token)) {
            index.get(token).add(hyperlink);
        }
        else {
            HashSet<String> urls = new HashSet<>();
            urls.add(hyperlink);
            index.put(token, urls);
        }
    }

    public void addURLConnections(String url, String hyperlink) {
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

    public RemissiveIndex() {
        this.index = new HashMap<>();
        this.webPages = new HashMap<>();
        this.urlConnection = new HashMap<>();
    }
}
