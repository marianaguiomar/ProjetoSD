import java.net.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Barrel extends Thread {
    //
    // parte do multicast
    //
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;

    public void run() {
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(PORT); // create socket and bind it
            InetAddress mcastaddr = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(new InetSocketAddress(mcastaddr, 0), NetworkInterface.getByIndex(0));


            while (true) {

                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                // vai receber um URL e as palavras associadas (+ título e citação de texto?)
                socket.receive(packet);

                //TODO -> colocar resultados na classe e no índice remissivo

                System.out.println("Received packet from " + packet.getAddress().getHostAddress() + ":"
                        + packet.getPort() + " with message:");
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println(message);
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
        private String[] tokens;

        public URL(String hiperlink, String titulo, String citacao, String[] tokens) {
            this.hiperlink = hiperlink;
            this.titulo = titulo;
            this.citacao = citacao;
            this.tokens = tokens;
        }
    }


    //
    // parte do índice remissivo
    //

    // String -> palavra, HashSet<String> -> URLs a que se relaciona

    public class IndiceRemissivo {
        private HashMap<String, HashSet<String>> index;

        public IndiceRemissivo(HashMap<String, HashSet<String>> index) {
            this.index = new HashMap<>();
        }

        public void add(URL url) {
            String tokens[] = url.tokens;
            String hiperlink = url.hiperlink;

            for (String token: tokens) {
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
    }



    //
    // main
    //

    public static void main(String[] args) {
        Barrel barrel = new Barrel();
        barrel.start();
    }
}
