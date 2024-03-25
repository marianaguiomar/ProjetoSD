import java.net.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Barrel {
    //
    // parte do multicast
    //
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;

    private IndiceRemissivo index;

    private MulticastSocket socket = null;

    public Barrel() throws IOException {
        socket = new MulticastSocket(PORT); // create socket and bind it
        index = new IndiceRemissivo();
    }


    public void printHashMap() {

        for (Map.Entry<String, HashSet<String>> entry : index.index.entrySet()) {
            String keyword = entry.getKey();
            HashSet<String> urls = entry.getValue();

            System.out.println("Keyword: " + keyword);
            System.out.println("URLs:");
            for (String url : urls) {
                System.out.println("  " + url);
            }
        }
    }

    public String receiveMessage() throws IOException {
        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        // vai receber um URL e as palavras associadas (+ título e citação de texto?)
        socket.receive(packet);

        //TODO -> colocar resultados na classe e no índice remissivo

        //System.out.println("Received packet from " + packet.getAddress().getHostAddress() + ":"
         //       + packet.getPort() + " with message:");
        String message = new String(packet.getData(), 0, packet.getLength());
        //System.out.println(message);

        return message;
    }

    public String getTitle(String message) {
        // Find the index of the first '|' character
        int index = message.indexOf('|');

        // If '|' is not found, return the entire message as the title
        if (index == -1) {
            return "";
        }

        // Extract the title from the beginning of the message up to '|' (excluding '|')
        return message.substring(0, index);
    }

    public String getURL(String message) {
        // Find the index of the first '|' character
        int index = message.indexOf('|');

        // If '|' is not found, return an empty string as there is no URL
        if (index == -1) {
            return "";
        }

        // Extract the URL from the message starting from the character after '|' (excluding '|')
        return message.substring(index + 1);
    }

    public String[] getTokens(String message) {
        String[] tokens = message.split(" ");
        return tokens;
    }


        public void run() {

        try {
            InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(new InetSocketAddress(mcastaddr, 0), NetworkInterface.getByIndex(0));


            while (true) {

                String message = receiveMessage();
                String titulo = getTitle(message);
                String url = getURL(message);

                message = receiveMessage();
                String citacao = message;

                URL urlObject = new URL(url, titulo, citacao);

                message = receiveMessage();
                while (message.charAt(0) != '§') {
                    String[] tokens = getTokens(message);
                    for (String token: tokens) {
                        index.add(urlObject, token);
                    }
                    message = receiveMessage();
                }

                printHashMap();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    //
    // classe URL
    //
    public class URL {
        private String hiperlink;
        private String titulo;
        private String citacao;

        public URL(String hiperlink, String titulo, String citacao) {
            this.hiperlink = hiperlink;
            this.titulo = titulo;
            this.citacao = citacao;
        }
    }


    //
    // parte do índice remissivo
    //

    // String -> palavra, HashSet<String> -> URLs a que se relaciona

    public class IndiceRemissivo {
        private HashMap<String, HashSet<String>> index;

        public IndiceRemissivo() {
            this.index = new HashMap<>();
        }

        public void add(URL url, String token) {
            String hiperlink = url.hiperlink;

            if (index.containsKey(token)) {
                index.get(token).add(hiperlink);
            }
            else {
                HashSet<String> hiperlinks = new HashSet<>();
                hiperlinks.add(hiperlink);
                index.put(token, hiperlinks);
            }
        }
    }



    //
    // main
    //

    public static void main(String[] args) throws IOException {
        Barrel barrel = new Barrel();
        barrel.run();
    }
}
