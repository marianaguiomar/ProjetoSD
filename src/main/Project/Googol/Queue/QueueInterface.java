package Googol.Queue;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for QUEUE Class
 */
public interface QueueInterface extends Remote, QueueSemaphoreInterface {

    /**
     * Method that inserts URL in queue
     * @param URL URL to be added
     */
    void addURL(String URL) throws RemoteException;

    /**
     * Method that fetches a URL from queue to be analysed by Downloader
     * @return URL from queue
     * @throws InterruptedException If the operation is interrupted
     */
    String fetchURL() throws RemoteException, InterruptedException;

    /**
     * Method that removes a downloader from the list of active downloader, also removing its interface, address and port
     * from the respective lists (calls method from DownloadManager)
     * @param address downloader's address
            * @param port downloader's port
            * @param ID downloader's id
            * @throws RemoteException If a remote communication error occurs.
     */
    void removeInstance(String address, int port, int ID) throws RemoteException;

    /**
     * Method that verifies if a given ID is available. If true, adds the instance to all lists
     * @param ID id to verify
     * @param address address of instance
     * @param port port of instance
     * @return true if ID is available
     * @throws RemoteException If a remote communication error occurs.
     */
    boolean verifyID(int ID, String address, int port) throws RemoteException;
    /**
     * Method that blocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    void block() throws RemoteException;
    /**
     * Method that unblocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    void unblock() throws RemoteException;


}
