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
import java.util.logging.Level;
import java.util.logging.Logger;

public class BarrelManager extends UnicastRemoteObject implements BarrelManagerInterface {
    private static final Logger LOGGER = Logger.getLogger(BarrelManager.class.getName());
    private static final String backupPath = "./src/main/Project/Googol/Gateway/BarrelManager/backup.dat";
    int activeBarrels;
    LinkedList<Integer> barrelsID;
    HashMap<Integer, String> barrelsAddresses;
    HashMap<Integer, Integer> barrelsPorts;
    HashMap<Integer, Boolean> isWorking;
    private HashMap<Integer, BarrelInterface> barrelsInterfaces;

    public BarrelManager(int port, String whitelistPath) throws RemoteException {
        super();
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            this.activeBarrels = 0;
            this.barrelsAddresses = new HashMap<>();
            this.barrelsInterfaces = new HashMap<>();
            this.barrelsAddresses = new HashMap<>();
            this.barrelsPorts = new HashMap<>();
            this.barrelsID = readWhitelist(whitelistPath);
            initializeIsWorking();
            registry.rebind("gateway", this);
            System.out.println("[PROJECTMANAGER#]:   " + "rmi://localhost:" + port + "/gateway");
            System.out.println("[PROJECTMANAGER#]:   Ready...");
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

    public BarrelInterface lookupBarrel(int barrelID) throws RemoteException {
        try {
            // Check if the barrel for the specified ID has already been looked up
            if (barrelsInterfaces.containsKey(barrelID)) {
                // If yes, return the already looked up barrel
                return barrelsInterfaces.get(barrelID);

            } else {
                // If not, perform the lookup
                BarrelInterface barrel = (BarrelInterface) Naming.lookup("rmi://"+ getBarrelAddres(barrelID)
                        + ":" + (barrelsPorts.get(barrelID)) + "/barrel" + barrelID);
                // Store the looked up barrel in the HashMap
                barrelsInterfaces.put(barrelID, barrel);
                return barrel;
            }
        } catch (NotBoundException | RemoteException | MalformedURLException e) {
             LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
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

    public String getBarrelAddres(int barrelID){
        return barrelsAddresses.get(barrelID);
    }

    public boolean verifyBarrelID(int ID, String barrelAddress, int barrelPort) throws RemoteException {
        LinkedList<Integer> linkedList = this.barrelsID;
        if (!linkedList.contains(ID)) {
            return false;
        }
        if (isWorking.get(ID))
            return false;
        isWorking.put(ID, true);
        barrelsAddresses.put(ID, barrelAddress);
        barrelsPorts.put(ID, barrelPort);
        activeBarrels++;
        System.out.println("[BARRELMANAGER]: Barrel#" + ID + " connected with address " + barrelAddress + ":"
                + barrelPort);
        return true;
    }

    public int getBarrelPort(int barrelID){
        return this.barrelsPorts.get(barrelID);
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
        activeBarrels--;
        if (activeBarrels == 0) {
            System.out.println("[PROJECTMANAGER#]: Last barrel removed, creating backup file");
            BarrelInterface barrel = lookupBarrel(barrelID);
            BackupManager.createBackupFile(barrel.getRemissiveIndex(), backupPath);
        }
        isWorking.put(barrelID, false);
        this.barrelsAddresses.remove(barrelID);
        this.barrelsPorts.remove(barrelID);
    }

    public int getBarrelID(int n) throws RemoteException {
        n = n % this.barrelsID.size();
        return this.barrelsID.get(n);
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


