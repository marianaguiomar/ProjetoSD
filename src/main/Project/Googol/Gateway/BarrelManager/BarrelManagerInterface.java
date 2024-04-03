package Googol.Gateway.BarrelManager;

import Googol.Barrel.RemissiveIndex;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface BarrelManagerInterface extends Remote {
    int createDownloaderID() throws RemoteException;
    boolean verifyBarrelID(int ID) throws RemoteException;

    int getActiveBarrels() throws RemoteException;
    int getAvailableBarrel(int n) throws RemoteException;
    RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException;

    LinkedList<Integer> getAvailableBarrelsID() throws RemoteException;
    int getBarrelID(int n) throws RemoteException;

    void removeBarrel(String barrelAddress, int barrelPort, int barrelID) throws RemoteException;
}
