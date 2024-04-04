package Googol.Manager.BarrelManager;

import Googol.Barrel.BarrelInterface;
import Googol.Barrel.RemissiveIndex;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface BarrelManagerInterface extends Remote {
    public boolean verifyID(int ID, String barrelAddress, int barrelPort) throws RemoteException;
    int getActiveInstances() throws RemoteException;
    int getAvailableBarrel(int n) throws RemoteException;
    RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException;
    public BarrelInterface lookupBarrel(int differentBarrelID) throws RemoteException;
    LinkedList<Integer> getAvailableBarrelsID() throws RemoteException;
    int getBarrelID(int n) throws RemoteException;

    void removeInstance(String barrelAddress, int barrelPort, int barrelID) throws RemoteException;
}
