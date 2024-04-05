package Googol.Gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for GATEWAY Class
 */
public interface GatewayInterface extends Remote {
    //TODO na gateway está isIntersectionSearch
    /**
     * Method that performs a search (communicating with the Barrels)
     * @param tokens okens to search for
     * @param pageNumber Page number (each page contains 10 results)
     * @param searchIntersection If true, intersection. If false, union
     * @return Set of 10 websites, according to the requested page
     * @throws RemoteException If a remote communication error occurs.
     */
    String search(String[] tokens, int pageNumber, boolean searchIntersection) throws RemoteException;

    /**
     * Method that returns the administrator informatin of the system (top10 searches performed,
     * average duration of search per barrel, active barrels
     * @return top10 searches performed, average duration of search per barrel, active barrels
     * @throws RemoteException If a remote communication error occurs.
     */
    String status() throws RemoteException;

    /**
     * Method that inserts a URL in the URLQueue
     * @param URL URL to be inserted
     * @throws RemoteException If a remote communication error occurs.
     */
    void insert(String URL) throws RemoteException;

    /**
     * Method that gets a Webpages connections, communicating with the barrels
     * @param URL URL
     * @return Webpage's connections
     * @throws RemoteException If a remote communication error occurs.
     */
    String getConnections(String URL) throws RemoteException;

}
