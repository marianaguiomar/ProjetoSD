import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Queue extends UnicastRemoteObject implements QueueInterface {
    LinkedBlockingQueue<String> URLQueue;
    public Queue() throws RemoteException {
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

}
