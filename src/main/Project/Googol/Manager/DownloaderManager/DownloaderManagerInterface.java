package Googol.Manager.DownloaderManager;
import java.rmi.RemoteException;
public interface DownloaderManagerInterface {
    void removeInstance(String address, int port, int ID) throws RemoteException;
}
