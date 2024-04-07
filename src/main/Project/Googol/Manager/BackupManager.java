package Googol.Manager;
import Googol.Barrel.RemissiveIndex;
import java.io.*;

/**
 * Class that manages the backup files
 */
public class BackupManager {
    /**
     * Mothod that creates a file, serializing a RemissiveIndex object
     * @param remissiveIndex RemissiveIndex
     * @param filename name of the destination file
     */
    public static void createBackupFile(RemissiveIndex remissiveIndex, String filename) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(remissiveIndex);
            objectOut.close();
            fileOut.close();
            //System.out.println("[BACKUPMANAGER#]: RemissiveIndex object has been serialized and saved in " + filename);
        }
        catch (IOException e) {
            //System.out.println("[BACKUPMANAGER#]:Failed to create backup file");
        }
    }

    /**
     * Mothod that derializes a RemissiveIndex object from a file
     * @param filename name of the source file
     */
    public static RemissiveIndex readBackupFile(String filename) {
        RemissiveIndex remissiveIndex;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            remissiveIndex = (RemissiveIndex) objectIn.readObject();
            objectIn.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            return new RemissiveIndex();
        }
        if (remissiveIndex == null)
            return new RemissiveIndex();
        return remissiveIndex;
    }
}
