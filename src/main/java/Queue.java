import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    LinkedBlockingQueue<String> URLQueue;
    private static final long serialVersionUID = 1L;

    public Queue(int port) throws RemoteException {
        super();
        this.URLQueue = new LinkedBlockingQueue<>();
        LocateRegistry.createRegistry(port).rebind("queue", this);

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
        QueueInterface queue = new Queue(1099);
        System.out.println("Queue Ready...");
    }
}
