package Googol.Manager.BarrelManager;
import Googol.Barrel.BarrelInterface;
import Googol.Barrel.RemissiveIndex;
import Googol.Manager.BackupManager;
import Googol.Manager.InstanceManager;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

public class BarrelManager extends InstanceManager implements BarrelManagerInterface {
    private final HashMap<Integer, BarrelInterface> barrelsInterfaces;

    public BarrelManager(int port, String whitelistPath) throws RemoteException {
        super(port, whitelistPath, "./src/main/Project/Googol/Manager/BarrelManager/backup.dat");
        this.barrelsInterfaces = new HashMap<>();
    }

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
    public boolean verifyID(int ID, String address, int port) throws RemoteException {
        LinkedList<Integer> linkedList = this.IDs;
        if (!linkedList.contains(ID)) {
            return false;
        }
        if (isWorking.get(ID))
            return false;
        isWorking.put(ID, true);
        addresses.put(ID, address);
        ports.put(ID, port);
        activeInstances++;
        System.out.println("[BARRELMANAGER]: Barrel#" + ID + " connected with address " + address + ":"
                + port);
        return true;
    }

    public RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException {
        if (activeInstances == 1) {
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
            System.out.println(differentBarrelID);
            if (barrel != null) {
                System.out.println("Failed to connect");
                remissiveIndex = barrel.getRemissiveIndex();
            }
            if (remissiveIndex == null) {
                System.out.println("Failed to retrieve");
                return new RemissiveIndex();
            }
            return remissiveIndex;
        }
    }


    public int getActiveInstances() throws RemoteException {
        return this.activeInstances;
    }

    public LinkedList<Integer> getAvailableBarrelsID() throws RemoteException {
        LinkedList<Integer> result = new LinkedList<>();
        for (Integer barrelID : IDs) {
            if (isWorking.get(barrelID)) {
                result.add(barrelID);
            }
        }
        return result;
    }


    public void removeInstance(String address, int port, int ID) throws RemoteException {
        System.out.println("[PROJECTMANAGER#]: Removing barrel with ID: " + ID);
        activeInstances--;
        if (activeInstances == 0) {
            System.out.println("[PROJECTMANAGER#]: Last barrel removed, creating backup file");
            BarrelInterface barrel = lookupBarrel(ID);
            BackupManager.createBackupFile(barrel.getRemissiveIndex(), backupPath);
        }
        isWorking.put(ID, false);
        this.addresses.remove(ID);
        this.ports.remove(ID);
    }

    public int getBarrelID(int n) throws RemoteException {
        n = n % this.IDs.size();
        return this.IDs.get(n);
    }

    public int getAvailableBarrel(int n) throws RemoteException {
        if (activeInstances == 0)
            return -1;
        n = n % this.activeInstances;
        int counter = 0;
        for (Integer barrelID : IDs) {
            if (isWorking.get(barrelID)) {
                System.out.println("[PROJECTMANAGER#]: Barrel " + barrelID + " is working");
                if (counter == n)
                    return barrelID;
                counter++;
            }
        }
        return -1;
    }
}


