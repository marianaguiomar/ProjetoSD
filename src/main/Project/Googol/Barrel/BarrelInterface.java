package Googol.Barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface BarrelInterface extends Remote {
    RemissiveIndex getRemissiveIndex() throws RemoteException;

    WebPage[] search(String[] tokens, Integer pageNumber, boolean intersection) throws RemoteException;
    String getConnections(String URL) throws RemoteException;

    int getBarrelNumber() throws RemoteException;
}
