package Googol.Multicast;

import java.io.*;
import java.util.UUID;
/**
 * Represents a multicast message containing information such as hyperlink, message type, payload, and message ID.
 * @param hyperlink Hyperlink to be sent.
 * @param messageID Unique ID of the message.
 * @param messageType Type of message.
 * @param payload Payload of the message.
 * @param activeBarrels Number of active barrels.
 */
public record MulticastMessage(String hyperlink, MessageType messageType, String payload , String messageID, Integer activeBarrels) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Class constructor, attributes are initialized
     * @param hyperlink hyperlink
     * @param messageType type of message
     * @param payload payload
     * @param activeBarrels number of active barrels
     */
    public MulticastMessage(String hyperlink, MessageType messageType, String payload,  Integer activeBarrels) {
        this(hyperlink, messageType, payload, generateUniqueId(), activeBarrels);
    }

    /**
     * Converts the message to a String
     * @return String with the message's info to a string
     */
    @Override
    public String toString() {
        return "MulticastMessage{" +
                "hyperlink='" + hyperlink + '\'' +
                ", messageType=" + messageType +
                ", payload='" + payload + '\'' +
                ", messageID='" + messageID + '\'' +
                ", activeBarrels=" + activeBarrels +
                '}';
    }

    /**
     * Converts the MulticastMessage object into a byte array.
     * @return Byte array representing the MulticastMessage object.
     * @throws IOException If an I/O error occurs while writing to the byte array.
     */
    public byte[] getBytes() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    /**
     * Generates a unique ID for the message.
     * @return A unique ID string.
     */
    public static String generateUniqueId() {
        // Get current timestamp
        long timestamp = System.currentTimeMillis();

        // Generate a random UUID
        UUID uuid = UUID.randomUUID();

        // Combine timestamp and UUID to create a unique ID

        return timestamp + "-" + uuid;
    }

    /**
     * Retrieves a MulticastMessage object from the given byte array.
     * @param data Byte array representing the MulticastMessage object.
     * @return MulticastMessage object retrieved from the byte array.
     */
    public static MulticastMessage getMessage(byte[] data){
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (MulticastMessage) ois.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}