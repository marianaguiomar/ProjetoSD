import java.rmi.RemoteException;

public interface QueueInterface {
    public void clearQueue() throws  RemoteException;
    public void addURL(String URL) throws RemoteException;
    public String fetchURL() throws RemoteException;
}
