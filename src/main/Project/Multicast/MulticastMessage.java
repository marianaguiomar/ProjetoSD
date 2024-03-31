package Multicast;

import java.io.*;
import java.util.UUID;
public record MulticastMessage(String hyperlink, MessageType messageType, String payload , String messageID) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public MulticastMessage(String hyperlink, MessageType messageType, String payload) {
        this(hyperlink, messageType, payload, generateUniqueId());
    }

    public byte[] getBytes() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static String generateUniqueId() {
        // Get current timestamp
        long timestamp = System.currentTimeMillis();

        // Generate a random UUID
        UUID uuid = UUID.randomUUID();

        // Combine timestamp and UUID to create a unique ID
        String uniqueId = timestamp + "-" + uuid.toString();

        return uniqueId;
    }

    public static MulticastMessage getMessage(byte[] data){
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (MulticastMessage) ois.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}