package Googol.Manager.BarrelManager;

import Googol.Barrel.BarrelInterface;
import Googol.Barrel.RemissiveIndex;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

/**
 * Interface for BARRELMANAGER Class
 */
public interface BarrelManagerInterface extends Remote {
    /**
     * Method that verifies if a given ID is available. If true, adds the instance to all lists
     * @param ID id to verify
     * @param address address of instance
     * @param port port of instance
     * @return true if ID is available
     * @throws RemoteException If a remote communication error occurs.
     */
    public boolean verifyID(int ID, String address, int port) throws RemoteException;

    /**
     * Method that returns number of active instances
     * @return number of active instances
     * @throws RemoteException If a remote communication error occurs.
     */
    int getActiveInstances() throws RemoteException;
    /**
     * Method that returns ID of the n-th available barrel
     * @param n position of the barrel
     * @return ID of the n-th available barrel
     * @throws RemoteException If a remote communication error occurs.
     */
    int getAvailableBarrel(int n) throws RemoteException;

    /**
     * Method that syncronizes a new barrel's remissive index with the others, or sets it up with the info on backup.dat
     * @param barrelID barrel id
     * @return remissive index
     * @throws RemoteException If a remote communication error occurs.
     */
    RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException;

    /**
     * Method that performs a lookup if it hasn't been performed. Else, it returns the already looked up barrel
     * @param differentBarrelID
     * @return Barrel interface connected to the specified barrel by RMI
     * @throws RemoteException If a remote communication error occurs.
     */
    public BarrelInterface lookupBarrel(int differentBarrelID) throws RemoteException;

    /**
     * Method that returns all available barrels' ids
     * @return all available barrels' ids
     * @throws RemoteException If a remote communication error occurs.
     */
    LinkedList<Integer> getAvailableBarrelsID() throws RemoteException;

    /**
     * Method that removes an instance from the list of active instances, also removing its interface, address and port
     * from the respective lists
     * @param barrelAddress instance's address
     * @param barrelPort instance's port
     * @param barrelID instance's id
     * @throws RemoteException If a remote communication error occurs.
     */
    void removeInstance(String barrelAddress, int barrelPort, int barrelID) throws RemoteException;
}
