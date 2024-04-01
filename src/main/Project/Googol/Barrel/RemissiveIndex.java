package Googol.Barrel;

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
        webPages.put(webPage.getHyperlink(), webPage);
    }
    public void printConnections() {
        for (Map.Entry<String, HashSet<String>> entry : urlConnection.entrySet()) {
            System.out.println("KEY: " + entry.getKey());
            for (String url : entry.getValue()) {
                System.out.println("  " + url);
            }
        }
    }
    public void printWebPages() {
        for (Map.Entry<String, WebPage> entry : webPages.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue().toString());
        }
    }

    public String getConnections(String hyperlink){
        String header = "CONNECTIONS TO LINK: " + hyperlink + "\n";
        StringBuilder result = new StringBuilder(header);
        if (urlConnection.containsKey(hyperlink)) {
            for (String url: urlConnection.get(hyperlink)) {
                result.append(webPages.get(url).toString());
            }
        }
        else{
            System.out.println("No connections found for link: " + hyperlink);
        }
        return result.toString();
    }
    public LinkedList<WebPage> findWebPagesUnion(String[] tokens){
        if(tokens == null)
            return new LinkedList<>();
        LinkedList<WebPage> result = new LinkedList<>();
        for (String token: tokens) {
            String searchToken = token.toLowerCase();
            if (index.containsKey(searchToken)) {
                HashSet<String> URLs = index.get(searchToken);
                Stream<WebPage> resultingWebPages = URLs.stream().filter(webPages::containsKey).map(webPages::get);
                resultingWebPages.forEach(result::add);
            }
        }
        return result;
    }

    public LinkedList<WebPage> findWebPagesIntersection(String[] tokens){
        LinkedList<WebPage> result = new LinkedList<>();

        // Initialize a set to store the web pages associated with the first token
        HashSet<String> firstTokenWebPages = new HashSet<>();
        String firstToken = tokens[0].toLowerCase();
        if (index.containsKey(firstToken)) {
            firstTokenWebPages.addAll(index.get(firstToken));
        } else {
            // If the first token is not found, return an empty list
            return result;
        }

        // Iterate through the remaining tokens
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].toLowerCase();
            if (index.containsKey(token)) {
                // Get the set of web pages associated with the current token
                HashSet<String> currentTokenWebPages = index.get(token);

                // Perform intersection with the set of web pages associated with the first token
                firstTokenWebPages.retainAll(currentTokenWebPages);
            } else {
                // If any token is not found, return an empty list
                return result;
            }
        }

        // Add the web pages from the intersection set to the result list
        for (String url : firstTokenWebPages) {
            if (webPages.containsKey(url)) {
                result.add(webPages.get(url));
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
        if(hyperlink == null || token == null){
            return;
        }
        if (index.containsKey(token)) {
            index.get(token).add(hyperlink);
        }
        else {
            HashSet<String> urls = new HashSet<>();
            urls.add(hyperlink);
            index.put(token, urls);
        }
    }

    public void insertWebPageCitation(String hyperlink, String citation) {
        if(hyperlink == null || citation == null){
            return;
        }
        if (webPages.containsKey(hyperlink)) {
            webPages.get(hyperlink).setCitation(citation);
        }
        else {
            WebPage webPage = new WebPage(hyperlink, "", citation);
            webPages.put(hyperlink, webPage);
        }
    }

    public void insertWebPageTitle(String hyperlink, String title) {
        if(hyperlink == null || title == null){
            return;
        }
        if (webPages.containsKey(hyperlink)) {
            webPages.get(hyperlink).setTitle(title);
        }
        else {
            WebPage webPage = new WebPage(hyperlink, title, "");
            webPages.put(hyperlink, webPage);
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
