package Googol.ProjectManager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ProjectManagerInterface extends Remote {
    int createNewID(boolean isDownloader) throws RemoteException;
    int getNumberOfBarrels() throws RemoteException;
}
