package Googol.Barrel;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for BARREL Class
 */
public interface BarrelInterface extends Remote {
    /**
     * Method that returns this barrel's RemissiveIndex
     * @return RemissiveIndex
     * @throws RemoteException If a remote communication error occurs.
     */
    RemissiveIndex getRemissiveIndex() throws RemoteException;

    /**
     * Method that performs a search based on given tokens and returns the websites that contain them
     * @param tokens Tokens to search for in the RemissiveIndex's keyset
     * @param pageNumber Page number (each page contains 10 results)
     * @param intersection If true, returns only pages that contain all tokens. If false, returns only pages that contain each token
     * @return Array of 10 websites, according to the requested page
     * @throws RemoteException If a remote communication error occurs.
     */
    WebPage[] search(String[] tokens, Integer pageNumber, boolean intersection) throws RemoteException;

    /**
     * Method that returns a given Webpage's connections
     * @param URL Webpage
     * @return number of connections
     * @throws RemoteException If a remote communication error occurs.
     */
    String getConnections(String URL) throws RemoteException;

    /**
     * Method that returns the Barrel number (id)
     * @return Barrel Number
     * @throws RemoteException If a remote communication error occurs.
     */
    int getMyID() throws RemoteException;
}
