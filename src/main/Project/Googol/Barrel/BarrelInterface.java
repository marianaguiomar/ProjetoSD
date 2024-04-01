package Googol.Barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface BarrelInterface extends Remote {

    WebPage[] searchUnion(String[] tokens, Integer pageNumber) throws RemoteException;
    WebPage[] searchIntersection(String[] tokens, Integer pageNumber) throws RemoteException;
    String status() throws RemoteException;
    String getConnections(String URL) throws RemoteException;
}
