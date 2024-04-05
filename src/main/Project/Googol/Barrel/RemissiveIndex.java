package Googol.Barrel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Class that manages a Remissive Index
 */
public class RemissiveIndex implements Serializable {
    /**
     * Index with a token and the URL's that contain it
     */
    public final HashMap<String, HashSet<String>> index;
    /**
     * All Webpages sent by Downloader
     */
    public final HashMap<String, WebPage> webPages;

    /**
     * Each Webpage's connections (other Webpages that reference them)
     */
    public final HashMap<String, HashSet<String>> urlConnection;


    /**
     * Method that returns a given Webpage's connections
     * @param hyperlink URL of a Webpage
     * @return String with Webpages
     */
    public String getConnections(String hyperlink){
        String header = "CONNECTIONS TO LINK: " + hyperlink + "\n";
        StringBuilder result = new StringBuilder(header);
        if (urlConnection.containsKey(hyperlink)) {
            for (String url: urlConnection.get(hyperlink)) {
                System.out.println(url);
                result.append(webPages.get(url).toString());
            }
        }
        return result.toString();
    }

    /**
     * Method that finds the Webpages that contain at least one of the given tokens in the Remissive Index
     * @param tokens Token to search for
     * @return List of Webpages
     */
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

    /**
     * Method that finds the Webpages that contain all the given tokens in the Remissive Index
     * @param tokens Token to search for
     * @return List of Webpages
     */
    public LinkedList<WebPage> findWebPagesIntersection(String[] tokens){
        LinkedList<WebPage> result = new LinkedList<>();

        // Initialize a set to store the web pages associated with the first token
        HashSet<String> firstTokenWebPages;
        String firstToken = tokens[0].toLowerCase();
        if (index.containsKey(firstToken)) {
            firstTokenWebPages = new HashSet<>(index.get(firstToken));
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

    /**
     * Method that finds a given Webpage's number of connections
     * @param hyperlink URL of a Webpage
     * @return A Webpage's number of connections
     */
    public int getNumberOfConnections(String hyperlink){
        if(!urlConnection.containsKey(hyperlink)){
            return 0;
        }
        return urlConnection.get(hyperlink).size();
    }

    /**
     * Method that adds a hyperlink that contains a token to the Remissive Index
     * Checks if neither are null
     * If token is already a key in Remissive Index, the URL is added to its values
     * Else, a new key is created for the token, the URL is added to its values
     * @param hyperlink URL to add
     * @param token token that the hyperlink contains
     */
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

    /**
     * Insert a Webpage's citation in the Webpages list
     * Checks if neither is null
     * If hyperlink is already a key in Webpages, find it and set its citation as the one given
     * Else, create a new Webpage and set it as a value, setting its citation as the one given
     * @param hyperlink URL of webpage
     * @param citation citation of webpage
     */
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

    /**
     * Insert a Webpage's title in the Webpage list
     * Checks if neither is null
     * If hyperlink is already a key in Webpages, find it and set its title as the one given
     * Else, create a new Webpage and set it as a value, setting its title as the one given
     * @param hyperlink URL of webpage
     * @param title title of webpage
     */
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

    /**
     * Insert a Webpage's citation in the URLConnections list
     * Checks if neither is null
     * If hyperlink is already a key in Webpages, find it and add given connection to its Connections
     * Else, create a new Webpage and set it as a value, setting its connections as a new set and adding given connection to it
     * @param url URL of main Webpage
     * @param hyperlink URLs of webpages that reference the main Webpage
     */
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

    /**
     * Class constructer, attributes are initialized
     */
    public RemissiveIndex() {
        this.index = new HashMap<>();
        this.webPages = new HashMap<>();
        this.urlConnection = new HashMap<>();
    }

    /**
     * Auxiliary method to print a barrel's Index
     * @param barrelNumber number of barrel
     */
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
}
