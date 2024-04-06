package Googol.Queue;

import java.io.Serializable;
import java.util.concurrent.Semaphore;
/**
 * Class that manages the queue acess using a binary semaphore.
 * The semaphore is blocked whenever there are no active barrels, so the queue is not accessed by the downloader (avoids unecessary work).
 */
public class QueueSemaphore implements Serializable {
    /**
     * Semaphore that blocks the queue
     */
    private final Semaphore queueSemaphore;

    /**
     * Class constructor, initializes the semaphore
     */
    public QueueSemaphore(){
        this.queueSemaphore = new Semaphore(1);
    }
    /**
     * Method that blocks the queue
     */
    public void block() {
        try {
            this.queueSemaphore.acquire();
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
    /**
     * Method that unblocks the queue
     */
    public void unblock() {
        this.queueSemaphore.release(); // Release the permit
    }
}
