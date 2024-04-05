package Googol.Queue;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

public class QueueSemaphore implements Serializable {
    private final Semaphore queueSemaphore = new Semaphore(100); // Initialize with one permit

    /**
     * Method that blocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    public void block() throws RemoteException{
        try {
            System.out.println("block");
            this.queueSemaphore.acquire();
        } catch (InterruptedException e) {
            return;
        }
    }
    /**
     * Method that unblocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    public void unblock() throws RemoteException{
        System.out.println("unblock");
        this.queueSemaphore.release(); // Release the permit
    }
    /**
     * Method that drains the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    public void drainSemaphore() throws RemoteException {
        this.queueSemaphore.drainPermits();
    }
    /**
     * Method that resets the queue's semaphore
     * @throws RemoteException If a remote communication error occurs.
     */
    public void resetSemaphore() throws RemoteException {
        this.queueSemaphore.release(500);
    }
}
