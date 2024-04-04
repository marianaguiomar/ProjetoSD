package Googol.Manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

public abstract class InstanceManager extends UnicastRemoteObject {
    protected final Logger LOGGER;
    protected int activeInstances;
    protected LinkedList<Integer> IDs;
    protected HashMap<Integer, String> addresses;
    protected HashMap<Integer, Integer> ports;
    protected HashMap<Integer, Boolean> isWorking;
    protected String instanceType;

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
            return whitelist;
        }
        return whitelist;
    }

    private void initializeIsWorking() {
        this.isWorking = new HashMap<>();
        for (Integer integer : IDs) {
            isWorking.put(integer, false);
        }
    }

    protected abstract void removeInstance(String address, int port, int ID) throws RemoteException;

    protected InstanceManager(String whitelistPath) throws RemoteException {
        this.LOGGER = Logger.getLogger(this.getClass().getName());
        this.addresses = new HashMap<>();
        this.ports = new HashMap<>();
        this.IDs = readWhitelist(whitelistPath);
        this.activeInstances = 0;
        this.addresses = new HashMap<>();
        initializeIsWorking();
    }
    public boolean verifyID(int ID, String address, int port) throws RemoteException {
        LinkedList<Integer> linkedList = this.IDs;
        if (!linkedList.contains(ID)) {
            return false;
        }
        if (isWorking.get(ID))
            return false;
        isWorking.put(ID, true);
        addresses.put(ID, address);
        ports.put(ID, port);
        activeInstances++;
        String objectType = this.getClass().getName().equals("DownloaderManager") ? "DOWNLOADER#" : "BARREL#";
        System.out.println(this.instanceType + ": "+ objectType + ID + " connected with address " + address + ":"
                + port);
        return true;
    }
}
