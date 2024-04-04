package Googol.Barrel;

import java.io.Serializable;

/**
 * Class that manages Webpages
 */
public class WebPage implements Serializable {
    /**
     * Hyperlink/URL
     */
    private String hyperlink;
    /**
     * Title
     */
    private String title;
    /**
     * Citation
     */
    private String citation;

    /**
     * Class constructer, attributes are initialized
     * @param hyperlink Hyperlink/URL
     * @param title Title
     * @param citation Citation
     */
    public WebPage(String hyperlink, String title, String citation) {
        this.hyperlink = hyperlink;
        this.title = title;
        this.citation = citation;
    }

    /**
     * Method that returns the Webpage's hyperlink
     * @return hyperlink
     */
    public String getHyperlink() {
        return hyperlink;
    }

    /**
     * Method that sets the Webpage's hyperlink
     * @param hyperlink hyperlink
     */
    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }

    /**
     * Method that returns the Webpage's title
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Method that sets the Webpage's hyperlink
     * @param title title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Method that returns the Webpage's citation
     * @return citation
     */
    public String getCitation() {
        return citation;
    }

    /**
     * Method that sets the Webpage's hyperlink
     * @param citation citation
     */
    public void setCitation(String citation) {
        this.citation = citation;
    }

    /**
     * Method that prints the Webpage's data
     * @return  Formatted string with the Webpage's data
     */
    @Override
    public String toString() {
        return String.format("TITLE: %s\n\tURL: %s\n\tCITATION: %s", title, hyperlink, citation);
    }
}