package Googol.ProjectManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
public class ProjectManager extends UnicastRemoteObject implements ProjectManagerInterface{
    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    int numberOfDownloaders;
    int activeBarrels;
    LinkedList<Integer> downloadersID;
    LinkedList<Integer> barrelsID;
    HashMap<Integer, Boolean> isWorking;

    private void initializeIsWorking(){
        this.isWorking = new HashMap<>();
        for (Integer integer : barrelsID) {
            isWorking.put(integer, false);
        }
    }
    public static LinkedList<Integer> readWhitelist(String filename) {
        LinkedList<Integer> whitelist = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    int number = Integer.parseInt(line.trim());
                    whitelist.add(number);
                } catch (NumberFormatException e) {
                    // Handle invalid integer format
                    System.err.println("Invalid integer format: " + line);
                }
            }
        } catch (IOException e) {
            // Handle file IO exception
            System.err.println("Error reading file: " + e.getMessage());
        }
        return whitelist;
    }
    public int createDownloaderID() throws RemoteException{
        LinkedList <Integer> linkedList = this.downloadersID ;
        Random random = new Random();

        int newID = random.nextInt(100) - 1;
        while(linkedList.contains(newID)){
            newID = random.nextInt(100) - 1;
        }
        this.numberOfDownloaders++;
        return newID;
    }
    public boolean verifyBarrelID(int ID) throws RemoteException{
        LinkedList <Integer> linkedList = this.barrelsID;
        if(!linkedList.contains(ID)){
            return false;
        }
        if(isWorking.get(ID))
            return false;
        isWorking.put(ID, true);
        activeBarrels++;
        return true;
    }

    public int getActiveBarrels() throws RemoteException{
        return this.activeBarrels;
    }

    public LinkedList<Integer> getAvailableBarrelsID() throws RemoteException {
        LinkedList<Integer> result = new LinkedList<>();
        for(Integer barrelID : barrelsID){
            if(isWorking.get(barrelID)){
                result.add(barrelID);
            }
        }
        return result;
    }

    public void removeBarrel(int barrelID) throws RemoteException {
        System.out.println("[PROJECTMANAGER#]: Removing barrel with ID: " + barrelID);
        barrelsID.remove((Integer) barrelID);
        activeBarrels--;
    }

    public int getBarrelID(int n)throws RemoteException {
        n = n % this.barrelsID.size();
        return this.barrelsID.get(n);
    }
    public void printBarrels(){
        for (Integer integer : barrelsID) {
            System.out.println(integer);
        }
    }

    public int getAvailableBarrel(int n) throws RemoteException {
        n = n % this.activeBarrels;
        int counter = 0;
        for (Integer barrelID : barrelsID) {
            if (isWorking.get(barrelID)) {
                System.out.println("[PROJECTMANAGER#]: Barrel " + barrelID + " is working");
                if(counter == n)
                    return barrelID;
                counter++;
            }
        }
        return -1;
    }
    public ProjectManager(Registry registry, String whitelistPath) throws RemoteException {
        super();
        try {
            this.numberOfDownloaders = 0;
            this.activeBarrels = 0;
            this.downloadersID = new LinkedList<>();
            this.barrelsID = readWhitelist("./src/main/Project/Googol/ProjectManager/whitelist.txt");
            initializeIsWorking();
            registry.rebind("projectManager", this);
            System.out.println("[PROJECTMANAGER#]:   Ready...");
            //printBarrels();
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws RemoteException {
        if(args.length != 2){
            System.out.println("Usage: java ProjectManager <port>");
            System.exit(1);
        }
        Registry registry = LocateRegistry.createRegistry(Integer.parseInt(args[0]));
        ProjectManagerInterface projectManager = new ProjectManager(registry, args[1]);
    }
}
