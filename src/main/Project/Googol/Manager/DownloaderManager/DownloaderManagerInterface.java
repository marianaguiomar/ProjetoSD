package Googol.Manager.DownloaderManager;
import java.rmi.RemoteException;

/**
 * Interface for DOWNLOADERMANAGER Class
 */
public interface DownloaderManagerInterface {

    /**
     * Method that removes an instance from the list of active instances, also removing its interface, address and port
     * from the respective lists
     * @param address instance's address
     * @param port instance's port
     * @param ID instance's id
     * @throws RemoteException If a remote communication error occurs.
     */
    void removeInstance(String address, int port, int ID) throws RemoteException;
}
