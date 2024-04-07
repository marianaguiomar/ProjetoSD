package Googol.Queue;

import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Class that manages the queue access using a binary semaphore.
 * The semaphore is blocked whenever there are no active barrels, so the queue is not accessed by the downloader (avoids unnecessary work).
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
        this.queueSemaphore = new Semaphore(0);
    }
    public boolean checkAvailability(){
        try {
            // Attempt to acquire the semaphore with a timeout of 1 second
            boolean acquired = this.queueSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            if(acquired){
                this.queueSemaphore.release();
                return true;
            }
            else {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }
    /**
     * Method that blocks the queue
     */
    public void block() {
        this.queueSemaphore.drainPermits();
    }
    /**
     * Method that unblocks the queue
     */
    public void unblock() {
        this.queueSemaphore.release(1);
    }
}
