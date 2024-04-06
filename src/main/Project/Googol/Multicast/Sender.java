package Googol.Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
/* Sender class for handling multicast messages.
 * <p>
 * Since Googol is a real-time application that cannot keep track of the packets it's sending, to maintain a reliable multicast machanism,
 * it is necessary to ensure the packet delivery by confirming the receipt of each packet individually.
 * For that matter, it is implemented a method known as Positive Acknowledgment with Retransmission (PAR) at the application level.
 * In this approach, each packet sent by the sender contains a sequence number (messageID), and the receiver acknowledges the receipt of each packet individually.
 * If the sender does not receive an acknowledgment for a packet within a specified timeout period, it retransmits that packet.
 *
 * */

/**
 * Sender class for handling multicast messages.
 */
public class Sender {
    /**
     * Max size of packet
     */
    private static final int PACKET_SIZE = 1500;
    private final int  numberOfRetries = 0;

    private final int numberOfMillisToWait = 1;
    /**
     * Timeout duration
     */
    private final Duration timeoutDuration;

    /**
     * Logger to print error messages
     */
    private static final Logger LOGGER = Logger.getLogger(Sender.class.getName());

    /**
     * Googol.Multicast address
     */
    private final String MULTICAST_ADDRESS;

    /**
     * Port for messages with a hyperlink's data
     */
    private final int PORT;

    /**
     * Socket for messages with a hyperlink's data
     */
    private MulticastSocket socket;

    /**
     * Port for ACKS
     */
    private MulticastSocket confirmationSocket;

    /**
     * Socket for ACKS
     */
    private final int CONFIRMATION_PORT;

    /**
     * Googol.Multicast group
     */
    InetAddress group;

    /**
     * Initializes the sender sockets.
     * socket sends URL data messages
     * confirmationSocket receives ACKS, so it jois the multicast group
     */
    private void initializeSenderSockets(){
        try {
            this.socket = new MulticastSocket();
            this.group = InetAddress.getByName(this.MULTICAST_ADDRESS);

            this.confirmationSocket = new MulticastSocket(this.CONFIRMATION_PORT);
            this.confirmationSocket.joinGroup(group);
            this.confirmationSocket.setSoTimeout(this.numberOfMillisToWait); // Timeout in milliseconds

        }
        catch (IOException | SecurityException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            throw new RuntimeException("Failed to create MulticastSocket");
        }
    }
    /**
     * Class constructor, attributes are initialized.
     * @param multicastAddress  Googol.Multicast address.
     * @param port              Port for sending multicast messages.
     * @param confirmationPort  Port for receiving confirmation messages.
     */
    public Sender(String multicastAddress, int port, int confirmationPort) {
        this.MULTICAST_ADDRESS = multicastAddress;
        this.PORT = port;
        this.CONFIRMATION_PORT = confirmationPort;
        this.timeoutDuration =  Duration.ofMillis(numberOfMillisToWait);
        initializeSenderSockets();
    }
    /**
     * Closes the sockets.
     */
    public void close(){
        this.socket.close();
        this.confirmationSocket.close();
    }

    /**
     * Waits for confirmation from the receiver.
     * Checks if ACKS are received within the defined timeout
     * Check if the number of ACKS of a message received are the same as the number of active barrels
     * Checks if message type is CONFIRMATION and the payload equals the ID of the message that was sent
     * @param sentMessageID ID of the message sent.
     * @return True if confirmation received within timeout, false otherwise.
     */
    private boolean waitForConfirmation(String sentMessageID){
        Instant startTime = Instant.now();
        Duration elapsedTime;
        try{
            int packCounter = 0;
            int activeBarrels = 2;
            // Wait for confirmation message during timeout period
            do{
                byte[] buffer = new byte[PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    confirmationSocket.receive(packet);
                } catch (SocketTimeoutException e) {
                    // If timeout occurs, return false
                    return false;
                }
                MulticastMessage message = MulticastMessage.getMessage(packet.getData());
                elapsedTime = Duration.between(startTime, Instant.now());
                if(message == null){
                    continue;
                }
                // Evalute message type and compare messageID with sent packet sequence number
                if(message.messageType() == MessageType.CONFIRMATION && message.payload().equals(sentMessageID)){
                    //Stem.out.println("received confirmation\n");
                    activeBarrels = message.activeBarrels();
                    packCounter++;
                }
            }
            while(elapsedTime.compareTo(timeoutDuration) < 0  && packCounter < activeBarrels);
            if (packCounter >= activeBarrels){
                //System.out.println("All barrels have confirmed the message");
            }
            return packCounter >= activeBarrels;
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends a multicast message.
     * Checks if the message isn't empty. Then, it sends the message through Googol.Multicast.
     * Waits for ACKS, if they aren't received, resends the message.
     * @param hyperlink    Hyperlink associated with the message.
     * @param payload      Payload of the message.
     * @param messageType  Type of the message.
     */
    public void sendMessage(String hyperlink, String payload, MessageType messageType){
        // Check if the message is empty
        if (hyperlink == null || hyperlink.isEmpty() || hyperlink.isBlank()|| payload == null || payload.isEmpty() || payload.isBlank()) {
            //Stem.out.println("Message is empty. Not sending anything.");
            return; // Exit the method if the message is empty
        }
        try{
            MulticastMessage message = new MulticastMessage(hyperlink, messageType, payload,0);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length, group, PORT);
            socket.send(packet);
            //System.out.println("Sent message" + message);
            // Keep sending package until confirmation is received
            int counter = 0;
            while (!waitForConfirmation(message.messageID()) && counter <= numberOfRetries){
                socket.send(packet);
                counter++;
            }
        }
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
        }

    }
}

