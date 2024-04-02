package Googol.ProjectManager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface ProjectManagerInterface extends Remote {
    int createNewID(boolean isDownloader) throws RemoteException;

    int getNumberOfBarrels() throws RemoteException;

    LinkedList<Integer> getBarrelsID() throws RemoteException;
    int getBarrelID(int n) throws RemoteException;

    void removeBarrel(int barrelID) throws RemoteException;
}
