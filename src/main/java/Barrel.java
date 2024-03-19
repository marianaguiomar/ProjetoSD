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
        MulticastSocket sendsocket = null;

        try {
            socket = new MulticastSocket(PORT); // create socket and bind it
            sendsocket = new MulticastSocket(PORT);


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



    //
    // parte do índice remissivo
    //

    // String -> palavra, HashSet<String> -> URLs a que se relaciona
    HashMap<String, HashSet<String>> index;


    //
    // main
    //

    public static void main(String[] args) {
        Barrel barrel = new Barrel();
        barrel.start();
    }
}
