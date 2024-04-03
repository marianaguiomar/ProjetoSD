package Googol.Gateway.BarrelManager;
import Googol.Barrel.RemissiveIndex;
import java.io.*;

public class BackupManager {

    // Method to serialize a RemissiveIndex object to a file
    public static void createBackupFile(RemissiveIndex remissiveIndex, String filename) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(remissiveIndex);
            objectOut.close();
            fileOut.close();
            System.out.println("RemissiveIndex object has been serialized and saved in " + filename);
        }
        catch (IOException e) {
            System.out.println("Failed to create backup file");
        }
    }

    // Method to deserialize a RemissiveIndex object from a file
    public static RemissiveIndex readBackupFile(String filename) {
        RemissiveIndex remissiveIndex;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            remissiveIndex = (RemissiveIndex) objectIn.readObject();
            objectIn.close();
            fileIn.close();
            System.out.println("RemissiveIndex object has been deserialized from " + filename);
        } catch (IOException | ClassNotFoundException e) {
            return new RemissiveIndex();
        }
        return remissiveIndex;
    }
}
