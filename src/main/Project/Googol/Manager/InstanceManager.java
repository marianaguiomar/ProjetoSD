package Googol.Manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class InstanceManager extends UnicastRemoteObject {
    protected final Logger LOGGER;
    protected String backupPath;
    protected int activeInstances;
    protected LinkedList<Integer> IDs;
    protected HashMap<Integer, String> addresses;
    protected HashMap<Integer, Integer> ports;
    protected HashMap<Integer, Boolean> isWorking;


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

    private void initializeIsWorking() {
        this.isWorking = new HashMap<>();
        for (Integer integer : IDs) {
            isWorking.put(integer, false);
        }
    }

    protected InstanceManager(int port, String whitelistPath, String backupPath) throws RemoteException {
        this.LOGGER = Logger.getLogger(this.getClass().getName());
        this.backupPath = backupPath;
        this.addresses = new HashMap<>();
        this.ports = new HashMap<>();
        this.IDs = readWhitelist(whitelistPath);

        Registry registry = LocateRegistry.createRegistry(port);
        this.activeInstances = 0;
        this.addresses = new HashMap<>();

        try {
            initializeIsWorking();
            registry.rebind("gateway", this);
            System.out.println("[PROJECTMANAGER#]:   " + "rmi://localhost:" + port + "/gateway");
            System.out.println("[PROJECTMANAGER#]:   Ready...");
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while initializing ProjectManager: " + e.getMessage(), e);
        }
    }
    public abstract boolean verifyID(int ID, String address, int port) throws RemoteException;
}
