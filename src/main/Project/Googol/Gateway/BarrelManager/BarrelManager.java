package Googol.Gateway.BarrelManager;
import Googol.Barrel.BarrelInterface;
import Googol.Barrel.RemissiveIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO -> download management goes to QUEUE
//TODO -> project manager becomes a thread of gateway
public class BarrelManager extends UnicastRemoteObject implements BarrelManagerInterface {
    private static final Logger LOGGER = Logger.getLogger(BarrelManager.class.getName());
    private static final String backupPath = "./src/main/Project/Googol/Gateway/BarrelManager/backup.dat";
    int numberOfDownloaders;
    int activeBarrels;
    LinkedList<Integer> downloadersID;
    LinkedList<Integer> barrelsID;
    HashMap<Integer, String> barrelsAddresses;
    HashMap<Integer, Boolean> isWorking;
    private static final HashMap<Integer, BarrelInterface> barrelsInterfaces = new HashMap<>();

    public BarrelManager(int port, String whitelistPath) throws RemoteException {
        super();
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            this.numberOfDownloaders = 0;
            this.activeBarrels = 0;
            this.downloadersID = new LinkedList<>();
            this.barrelsAddresses = new HashMap<>();
            this.barrelsID = readWhitelist(whitelistPath);
            initializeIsWorking();
            registry.rebind("gateway", this);
            System.out.println("[PROJECTMANAGER#]:   " + "rmi://localhost:" + port + "/gateway");
            System.out.println("[PROJECTMANAGER#]:   Ready...");
            //printBarrels();
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
    }

    private void initializeIsWorking() {
        this.isWorking = new HashMap<>();
        for (Integer integer : barrelsID) {
            isWorking.put(integer, false);
        }
    }

    public BarrelInterface lookupBarrel(int differentBarrelID) throws RemoteException {
        try {
            // Check if the barrel for the specified ID has already been looked up
            if (barrelsInterfaces.containsKey(differentBarrelID)) {
                // If yes, return the already looked up barrel
                System.out.println("slay1");
                return barrelsInterfaces.get(differentBarrelID);

            } else {
                // If not, perform the lookup
                System.out.println("slay2");
                BarrelInterface barrel = (BarrelInterface) Naming.lookup("rmi://"+ getBarrelAddres(differentBarrelID)
                        + ":" + (4400 + differentBarrelID) + "/barrel" + differentBarrelID);
                // Store the looked up barrel in the HashMap
                barrelsInterfaces.put(differentBarrelID, barrel);
                return barrel;
            }
        } catch (NotBoundException | RemoteException e) {
            e.printStackTrace();
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
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

    public int createDownloaderID() throws RemoteException {
        LinkedList<Integer> linkedList = this.downloadersID;
        Random random = new Random();

        int newID = random.nextInt(100) - 1;
        while (linkedList.contains(newID)) {
            newID = random.nextInt(100) - 1;
        }
        this.numberOfDownloaders++;
        return newID;
    }

    public String getBarrelAddres(int barrelID){
        return barrelsAddresses.get(barrelID);
    }

    public boolean verifyBarrelID(int ID, String barrelAddress) throws RemoteException {
        LinkedList<Integer> linkedList = this.barrelsID;
        if (!linkedList.contains(ID)) {
            return false;
        }
        if (isWorking.get(ID))
            return false;
        isWorking.put(ID, true);
        barrelsAddresses.put(ID, barrelAddress);
        activeBarrels++;
        System.out.println("[BARRELMANAGER]: Barrel#" + ID + " connect with addres " + barrelAddress);
        return true;
    }

    public RemissiveIndex setRemissiveIndex(int barrelID) throws RemoteException {
        if (activeBarrels == 1) {
            RemissiveIndex remissiveIndex = BackupManager.readBackupFile(backupPath);
            if (remissiveIndex == null)
                return new RemissiveIndex();
            return BackupManager.readBackupFile(backupPath);
        } else {
            RemissiveIndex remissiveIndex = null;
            int differentBarrelID = 1;
            for (Integer differentID : barrelsID) {
                if (differentID != barrelID && isWorking.get(differentID))
                    differentBarrelID = differentID;
            }
            // TODO -> criar hashmap com endereços dos barrel
            BarrelInterface barrel = lookupBarrel(differentBarrelID);
            System.out.println(differentBarrelID);
            if (barrel != null) {
                System.out.println("Failed to connect");
                remissiveIndex = barrel.getRemissiveIndex();
            }
            if (remissiveIndex == null) {
                System.out.println("Failed to retrieve");
                return new RemissiveIndex();
            }
            return remissiveIndex;
        }
    }


    public int getActiveBarrels() throws RemoteException {
        return this.activeBarrels;
    }

    public LinkedList<Integer> getAvailableBarrelsID() throws RemoteException {
        LinkedList<Integer> result = new LinkedList<>();
        for (Integer barrelID : barrelsID) {
            if (isWorking.get(barrelID)) {
                result.add(barrelID);
            }
        }
        return result;
    }


    public void removeBarrel(String barrelAddress, int barrelPort, int barrelID) throws RemoteException {
        System.out.println("[PROJECTMANAGER#]: Removing barrel with ID: " + barrelID);
        barrelsID.remove((Integer) barrelID);
        isWorking.put(barrelID, false);
        activeBarrels--;
        if (activeBarrels == 0) {
            System.out.println("[PROJECTMANAGER#]: Last barrel removed, creating backup file");

        }
        try {
            BarrelInterface barrel = (BarrelInterface) Naming.lookup("rmi://" + barrelAddress + ":" + barrelPort + "/barrel" + barrelID);
            BackupManager.createBackupFile(barrel.getRemissiveIndex(), backupPath);
        } catch (MalformedURLException | NotBoundException e) {
            LOGGER.log(Level.SEVERE, "Remote exception occurred\n " + e.getMessage(), e);
        }
    }

    public int getBarrelID(int n) throws RemoteException {
        n = n % this.barrelsID.size();
        return this.barrelsID.get(n);
    }

    public void printBarrels() {
        for (Integer integer : barrelsID) {
            System.out.println(integer);
        }
    }

    public int getAvailableBarrel(int n) throws RemoteException {
        if (activeBarrels == 0)
            return -1;
        n = n % this.activeBarrels;
        int counter = 0;
        for (Integer barrelID : barrelsID) {
            if (isWorking.get(barrelID)) {
                System.out.println("[PROJECTMANAGER#]: Barrel " + barrelID + " is working");
                if (counter == n)
                    return barrelID;
                counter++;
            }
        }
        return -1;
    }
}


