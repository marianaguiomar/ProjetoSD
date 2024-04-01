package Googol.ProjectManager;
import Googol.Gateway.Gateway;
import Googol.Queue.Queue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
public class ProjectManager extends UnicastRemoteObject implements ProjectManagerInterface{
    private final String MULTICAST_ADDRESS = "224.3.2.1";

    private final int PORT = 4321;
    private final String CONFIRMATION_MULTICAST_ADDRESS = "224.3.2.2";
    private final int CONFIRMATION_PORT = 4322;
    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    int numberOfDownloaders;
    int numberOfBarrels;
    HashSet<Integer> downloadersID;
    HashSet<Integer> barrelsID;

    public int createNewID(boolean isDownloader) throws RemoteException{
        HashSet <Integer> hashSet = isDownloader ? this.downloadersID : this.barrelsID;
        if(isDownloader)
            this.numberOfDownloaders++;
        else
            this.numberOfBarrels++;
        hashSet.add(this.numberOfBarrels);
        return this.numberOfBarrels;
    }

    public int getNumberOfBarrels() throws RemoteException{
        return this.numberOfBarrels;
    }

    public ProjectManager(Registry registry) throws RemoteException {
        super();
        try {
            this.numberOfDownloaders = 0;
            this.numberOfBarrels = 0;
            this.downloadersID = new HashSet<>();
            this.barrelsID = new HashSet<>();
            registry.rebind("projectManager", this);
            System.out.println("[PROJECTMANAGER#]:   Ready...");
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        if(args.length != 1){
            System.out.println("Usage: java ProjectManager <port>");
            System.exit(1);
        }
        Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
        ProjectManagerInterface projectManager = new ProjectManager(registry);
    }
}
