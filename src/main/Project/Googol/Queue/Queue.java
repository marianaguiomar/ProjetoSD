package Googol.Queue;

import Googol.Manager.BarrelManager.BarrelManager;
import Googol.Manager.DownloaderManager.DownloaderManager;
import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    LinkedBlockingQueue<String> URLQueue;
    DownloaderManager downloaderManager;

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(BarrelManager.class.getName());

    public Queue(Registry registryQueue) throws RemoteException {
        super();
        this.URLQueue = new LinkedBlockingQueue<>();
        downloaderManager = new DownloaderManager("./src/main/Project/Googol/Manager/DownloaderManager/whitelist");
        try {
            registryQueue.rebind("queue", this);
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
        }
        System.out.println("[QUEUE#]:   Ready...");

    }
    public void addURL(String URL) {
        this.URLQueue.add(URL);
    }
    public String fetchURL() throws InterruptedException {
        return this.URLQueue.take();
    }

    @Override
    public void removeInstance(String address, int port, int ID) throws RemoteException {
        this.downloaderManager.removeInstance(address, port, ID);
    }

    @Override
    public boolean verifyID(int ID, String address, int port) throws RemoteException {
        return this.downloaderManager.verifyID(ID, address, port);
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length != 1) {
            System.out.println("Usage: java Queue <port>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            Registry registryQueue = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
            QueueInterface queue = new Queue(registryQueue);
    }
        catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
    }
    }
}
