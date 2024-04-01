package Googol.Barrel;

import java.io.Serializable;

public class WebPage implements Serializable {
    private String hyperlink;
    private String title;
    private String citation;

    public WebPage(String hyperlink, String title, String citation) {
        this.hyperlink = hyperlink;
        this.title = title;
        this.citation = citation;
    }

    // Getters and setters for each attribute
    public String getHyperlink() {
        return hyperlink;
    }

    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    @Override
    public String toString() {
        return String.format("TITLE: %s\n\tURL: %s\n\tCITATION: %s", title, hyperlink, citation);
    }
}