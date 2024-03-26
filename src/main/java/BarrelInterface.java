import java.rmi.Remote;
import java.rmi.RemoteException;


public interface BarrelInterface extends Remote {

    WebPage[] search(String[] tokens, Integer pageNumber) throws RemoteException;
    String status() throws RemoteException;
    String getConnections(String URL) throws RemoteException;
}
