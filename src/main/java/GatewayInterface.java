import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GatewayInterface extends Remote {
    public String search(String[] tokens, int pageNumber) throws RemoteException;
    public String status() throws RemoteException;
    public void insert(String URL) throws RemoteException;
    public String getConnections(String URL) throws RemoteException;

}
