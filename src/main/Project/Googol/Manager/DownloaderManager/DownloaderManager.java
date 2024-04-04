package Googol.Manager.DownloaderManager;
import Googol.Barrel.BarrelInterface;
import Googol.Manager.BackupManager;
import Googol.Manager.InstanceManager;
import java.rmi.RemoteException;
import java.util.LinkedList;

public class DownloaderManager extends InstanceManager implements DownloaderManagerInterface  {
    protected DownloaderManager(int port, String whitelistPath, String backupPath) throws RemoteException {
        super(port, whitelistPath, backupPath);
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

    public void removeInstance(String address, int port, int ID) throws RemoteException {
        System.out.println("[PROJECTMANAGER#]: Removing barrel with ID: " + ID);
        activeInstances--;
        isWorking.put(ID, false);
        this.addresses.remove(ID);
        this.ports.remove(ID);
    }
}


