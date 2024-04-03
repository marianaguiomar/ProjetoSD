package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class Receiver{
    private static final int PACKET_SIZE = 1500;
    private static final Logger LOGGER = Logger.getLogger(Receiver.class.getName());
    private final String MULTICAST_ADDRESS;
    private final int PORT;
    private MulticastSocket socket;
    private final int CONFIRMATION_PORT;
    private MulticastSocket confirmationSocket;
    InetAddress group;
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
     * */

    public void sendConfirmationMulticastMessage(String hyperlink, String messageID) throws InterruptedException {
        try{
            MulticastMessage message = new MulticastMessage(hyperlink, MessageType.CONFIRMATION, messageID);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.group, CONFIRMATION_PORT);
            //Stem.out.println("Sent confirmation" + message.payload());
            this.confirmationSocket.send(packet);
        }
        catch(SocketException e) {
            sleep(10);
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }

    }
    // Method to process received packet
    public MulticastMessage receiveMessage(){
        try{
            byte[] buffer = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            // Receive data packets
            socket.receive(packet);
            MulticastMessage message = MulticastMessage.getMessage(packet.getData());
            assert message != null;
            // Send acknowledgment (confirmation) to the sender
            sendConfirmationMulticastMessage(message.hyperlink(), message.messageID());
            System.out.println("Received message " + message.messageID());
            return message;
        }
        catch (InterruptedException | IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            return null;
        }
    }
}
