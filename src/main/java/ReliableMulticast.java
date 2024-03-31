import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

// Follows Pragmatic General Multicast (PGM) algorithm.
// PGM is an IETF standard (RFC 3208) for reliable multicast transport of bulk data.

/*
* Since Googol is a real-time application that cannot keep track of the packets it's sending, to maintain a reliable multicast machanism,
* it is necessary to ensure the packet delivery, by confirming the receipt of each packet individually.
* For that matter, it is implemented a method known as Positive Acknowledgment with Retransmission (PAR) at the application level.
* In this approach, each packet sent by the sender contains a sequence number (messageID), and the receiver acknowledges the receipt of each packet individually.
* If the sender does not receive an acknowledgment for a packet within a specified timeout period, it retransmits that packet.
* */
public class ReliableMulticast {
    static LinkedBlockingQueue<MulticastMessage> sendMessageQueue;
    static LinkedBlockingQueue<MulticastMessage> receiveMessageQueue;
    private static final int PACKET_SIZE = 1500;
    private static final Duration timeoutDuration = Duration.ofSeconds(15);
    private static final Logger LOGGER = Logger.getLogger(ReliableMulticast.class.getName());

    public ReliableMulticast(String MULTICAST_ADDRESS, int PORT, String CONFIRMATION_MULTICAST_ADDRESS, int CONFIRMATION_PORT) {
        sendMessageQueue = new LinkedBlockingQueue<>();
        receiveMessageQueue = new LinkedBlockingQueue<>();
        // Create sender and receiver threads
        Thread senderThread = new Thread(new Sender(MULTICAST_ADDRESS, PORT, CONFIRMATION_PORT));
        Thread receiverThread = new Thread(new Receiver(MULTICAST_ADDRESS, PORT, CONFIRMATION_PORT));

        // Start sender and receiver threads
        senderThread.start();
        receiverThread.start();


    }


    // Create packet data with sequence number (messageID) and insert it in the internal message queue to later be sent
    public void sendMulticastMessage(String hyperlink, String payload, MessageType messageType){
        // Check if the message is empty
        if (hyperlink == null || hyperlink.isEmpty() || hyperlink.isBlank()|| payload == null || payload.isEmpty() || payload.isBlank()) {
            //Stem.out.println("Message is empty. Not sending anything.");
            return; // Exit the method if the message is empty
        }
        MulticastMessage message = new MulticastMessage(hyperlink, messageType, payload);
        sendMessageQueue.add(message);
    }
    public MulticastMessage receiveMulticastMessage() throws InterruptedException {
       return receiveMessageQueue.take();
    }


    public static class Sender implements Runnable {
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

        Sender(String multicastAddress, int port,  int confirmationPort) {
            this.MULTICAST_ADDRESS = multicastAddress;
            this.PORT = port;
            this.CONFIRMATION_PORT = confirmationPort;
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
        private void sendMessage(MulticastMessage message){

            try{
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer,buffer.length, group, PORT);
                socket.send(packet);
                //Stem.out.println("Sent message" + message.messageID());
                // Keep sending package until confirmation is received
                while (!waitForConfirmation(message.messageID())){
                    socket.send(packet);
                }
            }
            catch (IOException e){
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }

        }
        public void run(){
            while(true){
                try {
                    initializeSenderSockets();
                    MulticastMessage message = sendMessageQueue.take();
                    sendMessage(message);
                }
                catch (InterruptedException e){
                    LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
                }
            }
        }
    }
    public static class Receiver implements Runnable{
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

        Receiver(String multicastAddress, int port,  int confirmationPort) {
            this.MULTICAST_ADDRESS = multicastAddress;
            this.PORT = port;
            this.CONFIRMATION_PORT = confirmationPort;
        }

        /*
        * Method to send acknowledgment (confirmation) to the sender
        * Each packet sent by the sender contains a sequence number,
        * and the receiver acknowledges the receipt of each packet individually.
        * The sequence number is the MessageID, which is unique for each message.
        * Every confirmation message has the type MessageType.CONFIRMATION to distinguish it from other messages.
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
        public void receiveMessage(){
            try{
                byte[] buffer = new byte[PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                // Receive data packets
                socket.receive(packet);
                MulticastMessage message = MulticastMessage.getMessage(packet.getData());
                assert message != null;
                // Send acknowledgment (confirmation) to the sender
                sendConfirmationMulticastMessage(message.hyperlink(), message.messageID());
                //Stem.out.println("Received message" + message.messageID());
                receiveMessageQueue.add(message);
            }
            catch (InterruptedException | IOException e){
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }
        }

        public void run(){
            try {
                initializeReceiverSockets();
                while (true) {
                    receiveMessage();
                }
            }
            catch (Exception e){
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }
        }
    }
}
