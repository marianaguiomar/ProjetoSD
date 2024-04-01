package Googol;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GatewayInterface extends Remote {
    String search(String[] tokens, int pageNumber, boolean searchIntersection) throws RemoteException;
    String status() throws RemoteException;
    void insert(String URL) throws RemoteException;
    String getConnections(String URL) throws RemoteException;

}
