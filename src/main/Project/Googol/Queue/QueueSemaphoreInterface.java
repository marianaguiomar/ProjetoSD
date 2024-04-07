package Googol.Queue;

import java.rmi.RemoteException;
/**
 * Interface for QueueSemaphore Class
 */
public interface QueueSemaphoreInterface {
    /**
     * Method that blocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    void block() throws RemoteException;
    /**
     * Method that unblocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    void unblock() throws RemoteException;
}
