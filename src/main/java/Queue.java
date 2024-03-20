import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    LinkedBlockingQueue<String> URLQueue;
    private static final long serialVersionUID = 1L;
    public Queue() throws RemoteException {
        super();
        this.URLQueue = new LinkedBlockingQueue<>();

    }
    public void clearQueue(){
        this.URLQueue.clear();
    }
    public void addURL(String URL) {
        this.URLQueue.add(URL);
    }
    public String fetchURL(){
        return this.URLQueue.poll();
    }

    public static void main(String[] args) throws RemoteException {
        QueueInterface queue = new Queue();
        LocateRegistry.createRegistry(1099).rebind("queue", queue);
        System.out.println("Queue Ready...");
    }

}
