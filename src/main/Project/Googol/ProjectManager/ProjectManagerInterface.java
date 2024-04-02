package Googol.ProjectManager;

import com.sun.source.tree.Tree;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.UUID;

public interface ProjectManagerInterface extends Remote {
    int createNewID(boolean isDownloader) throws RemoteException;

    int getNumberOfBarrels() throws RemoteException;

    public LinkedList<Integer> getBarrelsID() throws RemoteException;
    public int getBarrelID(int n);

        public void removeBarrel(int barrelID);
}
