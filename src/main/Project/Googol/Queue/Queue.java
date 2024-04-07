package Googol.Queue;

import Googol.Manager.BarrelManager.BarrelManager;
import Googol.Manager.DownloaderManager.DownloaderManager;

import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * Class that manages the URLQueue
 */
public class Queue extends UnicastRemoteObject implements QueueInterface {
    /**
     * URLQueue
     */
    LinkedBlockingQueue<String> URLQueue;
    /**
     * Download manager
     */
    DownloaderManager downloaderManager;
    /**
     * Set of URL that have been visited
     */
    private final HashSet<String> visitedURL;
    /**
     * Semaphore to block and unblock the queue
     */

    private final QueueSemaphore queueSemaphore;
    /**
     * Serial version
     */

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Logger to print error messages
     */
    private static final Logger LOGGER = Logger.getLogger(BarrelManager.class.getName());

    /**
     * Class constructor, attributes are initialized
     * @param registryQueue Queue RMI registry
     * @param whitelistPath path to the downloader's whitelist
     * @throws RemoteException If a remote communication error occurs.
     */
    public Queue(Registry registryQueue, String whitelistPath) throws RemoteException {
        super();
        Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
        this.visitedURL = new HashSet<>();
        this.URLQueue = new LinkedBlockingQueue<>();

        downloaderManager = new DownloaderManager(whitelistPath);
        try {
            registryQueue.rebind("queue", this);
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
        }
        this.queueSemaphore = new QueueSemaphore();
        System.out.println("[QUEUE#]:   Ready...");

    }

    /**
     * Method that verifies if URL was visited already, and then inserts URL in queue
     * @param URL URL to be added
     */
    public void addURL(String URL) {
        if(!visitedURL.contains(URL)) {
            visitedURL.add(URL);
            this.URLQueue.add(URL);
        }
    }

    /**
     * Method that fetches a URL from queue to be analysed by Downloader
     * @return URL from queue
     */
    public String fetchURL() throws RemoteException {
        String result;
        try {

            if (!this.queueSemaphore.checkAvailability() ){
                return null;
            }

            result = URLQueue.poll(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while fetching URL: \n" + e.getMessage(), e);
            return null;
        }
        return result;
    }

    private void exit(){
        try {
            sleep(1000);
            System.out.println("[QUEUE#]: Exited...");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while exiting Queue: \n" + e.getMessage(), e);
        }
    }

    /**
     * Method that removes a downloader from the list of active downloader, also removing its interface, address and port
     * from the respective lists (calls method from DownloadManager)
     * @param address downloader's address
     * @param port downloader's port
     * @param ID downloader's id
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public void removeInstance(String address, int port, int ID) throws RemoteException {
        this.downloaderManager.removeInstance(address, port, ID);
    }

    /**
     * Method that communicates with downlaodManager in gateway
     *  to verify if a given ID is available. If true, adds the instance to all lists
     * @param ID id to verify
     * @param address address of instance
     * @param port port of instance
     * @return true if ID is available
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public boolean verifyID(int ID, String address, int port) throws RemoteException {
        return this.downloaderManager.verifyID(ID, address, port);
    }

    @Override
    public void block() throws RemoteException {
        this.queueSemaphore.block();
    }

    @Override
    public void unblock() throws RemoteException {
        this.queueSemaphore.unblock();
    }

    /**
     * Main method that initializes the Queue
     * @param args port and whitelist path
     * @throws RemoteException If a remote communication error occurs.
     */
    public static void main(String[] args) throws RemoteException {
        if (args.length != 2) {
            System.out.println("Usage: java Queue <port> <downloaderWhitelistPath>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            Registry registryQueue = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
            QueueInterface queue = new Queue(registryQueue, args[1]);
    }
        catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
     }
    }

}
