package Googol.ProjectManager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ProjectManagerInterface extends Remote {
    int createDownloaderID() throws RemoteException;
    boolean verifyBarrelID(int ID) throws RemoteException;

    int getActiveBarrels() throws RemoteException;
    int getAvailableBarrel(int n) throws RemoteException;

    LinkedList<Integer> getAvailableBarrelsID() throws RemoteException;
    int getBarrelID(int n) throws RemoteException;

    void removeBarrel(int barrelID) throws RemoteException;
}
