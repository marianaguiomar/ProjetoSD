package Googol.Queue;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface QueueInterface extends Remote {
    void addURL(String URL) throws RemoteException;
    String fetchURL() throws RemoteException, InterruptedException;

    void removeInstance(String address, int port, int ID) throws RemoteException;

    boolean verifyID(int ID, String address, int port) throws RemoteException;
}
