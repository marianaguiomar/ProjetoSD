package Googol.Queue;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.rmi.registry.Registry;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    LinkedBlockingQueue<String> URLQueue;
    private static final long serialVersionUID = 1L;

    public Queue(Registry registry) throws RemoteException {
        super();
        this.URLQueue = new LinkedBlockingQueue<>();
        try {
            registry.rebind("queue", this);

        } catch (RemoteException e) {
            //String rmiAddress = "rmi://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT + "/barrel" + barrelNumber;
            //System.out.println("[BARREL#" + barrelNumber + "]:" + "    RMI Address: " + rmiAddress);
            e.printStackTrace();
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
        try {
            // Create RMI registry
            Registry registry = LocateRegistry.createRegistry(1099);
            QueueInterface queue = new Queue(registry);
    }
        catch (RemoteException e) {
        e.printStackTrace();
    }
    }
}
