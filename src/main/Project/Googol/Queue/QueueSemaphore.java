package Googol.Queue;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.Semaphore;

public class QueueSemaphore implements Serializable {
    private final Semaphore queueSemaphore;


    public QueueSemaphore(){
        this.queueSemaphore = new Semaphore(1);
    }
    /**
     * Method that blocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    public void block() throws RemoteException{
        try {
            this.queueSemaphore.acquire();
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
    /**
     * Method that unblocks the queue
     * @throws RemoteException If a remote communication error occurs.
     */
    public void unblock() throws RemoteException{
        this.queueSemaphore.release(); // Release the permit
    }
}
