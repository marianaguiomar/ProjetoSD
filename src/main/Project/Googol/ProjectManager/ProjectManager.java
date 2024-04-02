package Googol.ProjectManager;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
public class ProjectManager extends UnicastRemoteObject implements ProjectManagerInterface{
    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    int numberOfDownloaders;
    int numberOfBarrels;
    LinkedList<Integer> downloadersID;
    LinkedList<Integer> barrelsID;

    public int createNewID(boolean isDownloader) throws RemoteException{
        LinkedList <Integer> linkedList = isDownloader ? this.downloadersID : this.barrelsID;
        Random random = new Random();

        int newID = random.nextInt(100) - 1;
        while(linkedList.contains(newID)){
            newID = random.nextInt(100) - 1;
        }

        if(isDownloader) {
            this.numberOfDownloaders++;
        }
        else{
            this.numberOfBarrels++;
        }
        linkedList.add(newID);
        return newID;


    }

    public int getNumberOfBarrels() throws RemoteException{
        return this.numberOfBarrels;
    }

    public LinkedList<Integer> getBarrelsID() throws RemoteException {
        return this.barrelsID;
    }

    public void removeBarrel(int barrelID) throws RemoteException {
        System.out.println("[PROJECTMANAGER#]: Removing barrel with ID: " + barrelID);
        barrelsID.remove(barrelsID.indexOf(barrelID));
        numberOfBarrels--;
    }

    public int getBarrelID(int n)throws RemoteException {
        n = n % this.barrelsID.size();
        return this.barrelsID.get(n);
    }

    public ProjectManager(Registry registry) throws RemoteException {
        super();
        try {
            this.numberOfDownloaders = 0;
            this.numberOfBarrels = 0;
            this.downloadersID = new LinkedList<>();
            this.barrelsID = new LinkedList<>();
            registry.rebind("projectManager", this);
            System.out.println("[PROJECTMANAGER#]:   Ready...");
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws RemoteException {
        if(args.length != 1){
            System.out.println("Usage: java ProjectManager <port>");
            System.exit(1);
        }
        Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
        ProjectManagerInterface projectManager = new ProjectManager(registry);
    }
}
