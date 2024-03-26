import java.io.Serializable;

public record WebPage(String hiperlink, String titulo, String citacao) implements Serializable {
    @Override
    public String toString() {
        return String.format("TITLE: %s\n\tURL: %s\n\tCITATION: %s",titulo,hiperlink, citacao);
    }
}
