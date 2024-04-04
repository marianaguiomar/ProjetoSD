package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
/* Receiver class for handling multicast messages.
 * <p>
 * Since Googol is a real-time application that cannot keep track of the packets it's sending, to maintain a reliable multicast machanism,
 * it is necessary to ensure the packet delivery by confirming the receipt of each packet individually.
 * For that matter, it is implemented a method known as Positive Acknowledgment with Retransmission (PAR) at the application level.
 * In this approach, each packet sent by the sender contains a sequence number (messageID), and the receiver acknowledges the receipt of each packet individually.
 * If the sender does not receive an acknowledgment for a packet within a specified timeout period, it retransmits that packet.
 *
 */
public class Receiver{
    private static final int PACKET_SIZE = 1500;
    private static final Logger LOGGER = Logger.getLogger(Receiver.class.getName());
    private final String MULTICAST_ADDRESS;
    private final int PORT;
    private MulticastSocket socket;
    private final int CONFIRMATION_PORT;
    private MulticastSocket confirmationSocket;
    InetAddress group;
    /**
     * Receiver class for handling multicast messages.
     */
    private void initializeReceiverSockets(){
        try {
            this.socket = new MulticastSocket(this.PORT); // Use the same port used for sending
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS); // Use the same multicast group address used for sending
            socket.joinGroup(group);

            this.confirmationSocket = new MulticastSocket();
        }
        catch (IOException | SecurityException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            throw new RuntimeException("Failed to create MulticastSocket");
        }
    }
    /**
     * Constructor for Receiver class.
     *
     * @param multicastAddress Multicast address.
     * @param port             Port for receiving multicast messages.
     * @param confirmationPort Port for sending confirmation messages.
     */
    public Receiver(String multicastAddress, int port, int confirmationPort) {
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = port;
        this.CONFIRMATION_PORT = confirmationPort;
        initializeReceiverSockets();
    }

    /*
     * Method to send acknowledgment (confirmation) to the sender
     * Each packet sent by the sender contains a sequence number,
     * and the receiver acknowledges the receipt of each packet individually.
     * The sequence number is the MessageID, which is unique for each message.
     * Every confirmation message has the type Multicast.MessageType.CONFIRMATION to distinguish it from other messages.
     *
     * @param hyperlink  Hyperlink to be confirmed.
     * @param messageID  ID of the message being confirmed.
     * @throws InterruptedException If the thread is interrupted.
     */

    public void sendConfirmationMulticastMessage(String hyperlink, String messageID, int activeBarrels) throws InterruptedException {
        try{
            MulticastMessage message = new MulticastMessage(hyperlink, MessageType.CONFIRMATION, messageID, "",activeBarrels);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, CONFIRMATION_PORT);
            System.out.println(message);
            this.confirmationSocket.send(packet);
        }
        catch(SocketException e) {
            sleep(10);
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }

    }
    /**
     * Receives a multicast message.
     *
     * @return Received multicast message.
     */
    public MulticastMessage receiveMessage(int activeBarrels){
        try{
            byte[] buffer = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            // Receive data packets
            socket.receive(packet);
            MulticastMessage message = MulticastMessage.getMessage(packet.getData());
            assert message != null;
            // Send acknowledgment (confirmation) to the sender
            sendConfirmationMulticastMessage(message.hyperlink(), message.messageID(), activeBarrels);
            //System.out.println("Received message " + message.messageID());
            return message;
        }
        catch (InterruptedException | IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            return null;
        }
    }
}
