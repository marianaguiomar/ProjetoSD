import java.rmi.Remote;
import java.rmi.RemoteException;

public interface QueueInterface extends Remote {
    public void clearQueue() throws  RemoteException;
    public void addURL(String URL) throws RemoteException;
    public String fetchURL() throws RemoteException, InterruptedException;
}
