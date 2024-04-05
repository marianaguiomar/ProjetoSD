package Googol.Manager.BarrelManager;
import Googol.Barrel.BarrelInterface;
import Googol.Barrel.RemissiveIndex;
import Googol.Manager.BackupManager;
import Googol.Manager.InstanceManager;
import Googol.Queue.QueueInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Class that manages all barrels
 */
public class BarrelManager extends InstanceManager implements BarrelManagerInterface {
    /**
     * Hash map with all barrel interfaces
     */
    private final HashMap<Integer, BarrelInterface> barrelsInterfaces;

    /**
     * Path to backup file
     */
    private final String backupPath;

    private final QueueInterface queue;

    /**
     * Class constructer, attributes are initialized
     * @param port Barrel manager port
     * @param whitelistPath Path to the barrel's whitelist
     * @throws RemoteException If a remote communication error occurs.
     */
    public BarrelManager(int port, String whitelistPath, QueueInterface queueInterface) throws RemoteException {
        super(whitelistPath);
        this.instanceType = "[BARRELMANAGER#]";
        this.queue = queueInterface;
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("gateway", this);
            System.out.println(this.instanceType + ":   " + "rmi://localhost:" + port + "/gateway");
            System.out.println(this.instanceType + ":   Ready...");
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
        this.backupPath = "./src/main/Project/Googol/Manager/BarrelManager/backup.dat";
        this.barrelsInterfaces = new HashMap<>();
    }

    /**
     * Method that performs a lookup if it hasn't been performed. Else, it returns the already looked up barrel
     * @param barrelID Barrel id
     * @return Barrel interface connected to the specified barrel by RMI
     * @throws RemoteException If a remote communication error occurs.
     */
    public BarrelInterface lookupBarrel(int barrelID) throws RemoteException {
        try {
            // Check if the barrel for the specified ID has already been looked up
            if (barrelsInterfaces.containsKey(barrelID)) {
                // If yes, return the already looked up barrel
                return barrelsInterfaces.get(barrelID);

            } else {
                // If not, perform the lookup
                BarrelInterface barrel = (BarrelInterface) Naming.lookup("rmi://"+ addresses.get(barrelID)
                        + ":" + (ports.get(barrelID)) + "/barrel" + barrelID);
                // Store the looked up barrel in the HashMap
                barrelsInterfaces.put(barrelID, barrel);
                return barrel;
            }
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
             LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Method that syncronizes a new barrel's remissive index with the others, or sets it up with the info on backup.dat
     * @param barrelID barrel id
     * @return remissive index
     * @throws RemoteException If a remote communication error occurs.
     */
    public RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException {
        if (activeInstances == 1) {
            this.queue.unblock();
            RemissiveIndex remissiveIndex = BackupManager.readBackupFile(backupPath);
            if (remissiveIndex == null)
                return new RemissiveIndex();
            return BackupManager.readBackupFile(backupPath);
        } else {
            RemissiveIndex remissiveIndex = null;
            int differentBarrelID = 1;
            for (Integer differentID : IDs) {
                if (differentID != barrelID && isWorking.get(differentID))
                    differentBarrelID = differentID;
            }
            BarrelInterface barrel = lookupBarrel(differentBarrelID);
            if (barrel != null) {
                remissiveIndex = barrel.getRemissiveIndex();
            }
            if (remissiveIndex == null) {
                System.out.println("Failed to retrieve");
                return new RemissiveIndex();
            }
            return remissiveIndex;
        }
    }


    /**
     * Method that returns all available barrels' ids
     * @return all available barrels' ids
     * @throws RemoteException If a remote communication error occurs.
     */
    public LinkedList<Integer> getAvailableBarrelsID() throws RemoteException {
        LinkedList<Integer> result = new LinkedList<>();
        for (Integer barrelID : IDs) {
            if (isWorking.get(barrelID)) {
                result.add(barrelID);
            }
        }
        return result;
    }


    /**
     * Method that removes a barrel from the list of active barrels, also removing its interface, address and port
     * from the respective lists
     * @param address barrel's address
     * @param port barrel's port
     * @param ID barrel's id
     * @throws RemoteException If a remote communication error occurs.
     */
    public void removeInstance(String address, int port, int ID) throws RemoteException {
        System.out.println(this.instanceType + ": Removing barrel with ID: " + ID);
        activeInstances--;
        if (activeInstances == 0) {
            System.out.println(this.instanceType + ": Last barrel removed, creating backup file");
            this.queue.block();
            BarrelInterface barrel = lookupBarrel(ID);
            BackupManager.createBackupFile(barrel.getRemissiveIndex(), backupPath);
        }
        barrelsInterfaces.remove(ID);
        isWorking.put(ID, false);
        this.addresses.remove(ID);
        this.ports.remove(ID);
    }


    /**
     * Method that returns ID of the n-th available barrel
     * @param n position of the barrel
     * @return ID of the n-th available barrel
     * @throws RemoteException If a remote communication error occurs.
     */
    public int getAvailableBarrel(int n) throws RemoteException {
        if (activeInstances == 0)
            return -1;
        n = n % this.activeInstances;
        int counter = 0;
        for (Integer barrelID : IDs) {
            if (isWorking.get(barrelID)) {
                //System.out.println(this.instanceType + ": Available BARREL#" + barrelID);
                if (counter == n)
                    return barrelID;
                counter++;
            }
        }
        return -1;
    }
}


