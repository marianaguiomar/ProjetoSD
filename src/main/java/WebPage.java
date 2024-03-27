import java.io.Serializable;
import java.util.HashSet;

public record WebPage(String hyperlink, String titulo, String citacao, HashSet<String> mentionsURL) implements Serializable {
    @Override
    public String toString() {
        return String.format("TITLE: %s\n\tURL: %s\n\tCITATION: %s",titulo, hyperlink, citacao);
    }
}