package Googol.Queue;

import Googol.Gateway.BarrelManager.BarrelManager;

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
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(BarrelManager.class.getName());

    public Queue(Registry registry) throws RemoteException {
        super();
        this.URLQueue = new LinkedBlockingQueue<>();
        try {
            registry.rebind("queue", this);

        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
        }
        System.out.println("[QUEUE#]:   Ready...");

    }
    public void clearQueue(){
        this.URLQueue.clear();
    }
    public void addURL(String URL) {
        this.URLQueue.add(URL);
    }
    public String fetchURL() throws InterruptedException {
        return this.URLQueue.take();
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length != 1) {
            System.out.println("Usage: java Queue <port>");
            System.exit(1);
        }
        try {
            // Create RMI registry
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
            QueueInterface queue = new Queue(registry);
    }
        catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing Queue: \n" + e.getMessage(), e);
    }
    }
}
