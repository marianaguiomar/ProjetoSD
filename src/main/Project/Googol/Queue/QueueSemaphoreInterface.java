package Googol.Queue;

import java.rmi.RemoteException;

public interface QueueSemaphoreInterface {
    void block() throws RemoteException;
    void unblock() throws RemoteException;
    void drainSemaphore() throws RemoteException;
    void resetSemaphore() throws RemoteException;
}
