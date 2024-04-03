package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
// Follows Pragmatic General Multicast (PGM) algorithm.
// PGM is an IETF standard (RFC 3208) for reliable multicast transport of bulk data.

/*
 * Since Googol is a real-time application that cannot keep track of the packets it's sending, to maintain a reliable multicast machanism,
 * it is necessary to ensure the packet delivery, by confirming the receipt of each packet individually.
 * For that matter, it is implemented a method known as Positive Acknowledgment with Retransmission (PAR) at the application level.
 * In this approach, each packet sent by the sender contains a sequence number (messageID), and the receiver acknowledges the receipt of each packet individually.
 * If the sender does not receive an acknowledgment for a packet within a specified timeout period, it retransmits that packet.
 * */
public class Sender {
    private static final int PACKET_SIZE = 1500;
    private static final Duration timeoutDuration = Duration.ofSeconds(15);
    private static final Logger LOGGER = Logger.getLogger(Sender.class.getName());
    private final String MULTICAST_ADDRESS;
    private final int PORT;
    private MulticastSocket socket;
    private MulticastSocket confirmationSocket;
    private final int CONFIRMATION_PORT;
    InetAddress group;

    private void initializeSenderSockets(){
        try {
            this.socket = new MulticastSocket();
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);

            this.confirmationSocket = new MulticastSocket(this.CONFIRMATION_PORT);
            this.confirmationSocket.joinGroup(group);

        }
        catch (IOException | SecurityException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            throw new RuntimeException("Failed to create MulticastSocket");
        }
    }

    public Sender(String multicastAddress, int port, int confirmationPort) {
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = port;
        this.CONFIRMATION_PORT = confirmationPort;
        initializeSenderSockets();
    }

    // Method to process acknowledgment (confirmation) from the receiver
    private boolean waitForConfirmation(String sentMessageID){
        Instant startTime = Instant.now();
        Duration elapsedTime;
        try{
            // Wait for confirmation message during timeout period
            do{
                byte[] buffer = new byte[PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                confirmationSocket.receive(packet);
                MulticastMessage message = MulticastMessage.getMessage(packet.getData());
                assert message != null;
                //Stem.out.println("Reived confirmation" + message.payload());
                // Evalute message type and compare messageID with sent packet sequence number
                if(message.messageType() == MessageType.CONFIRMATION && message.payload().equals(sentMessageID)){
                    //Stem.out.println("received confirmation\n");
                    return true;
                }
                elapsedTime = Duration.between(startTime, Instant.now());

            }
            while(elapsedTime.compareTo(timeoutDuration) < 0);
            //If the confirmation message is not receive, resend package
            return false;
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            return false;
        }
    }
    // Method to send data packet
    public void sendMessage(String hyperlink, String payload, MessageType messageType){
        // Check if the message is empty
        if (hyperlink == null || hyperlink.isEmpty() || hyperlink.isBlank()|| payload == null || payload.isEmpty() || payload.isBlank()) {
            //Stem.out.println("Message is empty. Not sending anything.");
            return; // Exit the method if the message is empty
        }
        try{
            MulticastMessage message = new MulticastMessage(hyperlink, messageType, payload);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length, group, PORT);
            socket.send(packet);
            //System.out.println("Sent message" + message);
            // Keep sending package until confirmation is received
            while (!waitForConfirmation(message.messageID())){
                socket.send(packet);
            }
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }

    }
}

