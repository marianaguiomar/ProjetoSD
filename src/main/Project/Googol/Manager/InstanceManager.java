package Googol.Manager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Superclass that manages instance managers (downloader, barrel)
 */
public abstract class InstanceManager extends UnicastRemoteObject {
    /**
     * Logger to print error messages
     */
    protected final Logger LOGGER;

    /**
     * Number of active instances
     */
    protected int activeInstances;

    /**
     * IDs of instances given by the whitelist
     */
    protected LinkedList<Integer> IDs;

    /**
     * Addresses of active instances
     */
    protected HashMap<Integer, String> addresses;

    /**
     * Ports of active instances
     */
    protected HashMap<Integer, Integer> ports;

    /**
     * List that keeps track of wether an instace is working or not
     */
    protected HashMap<Integer, Boolean> isWorking;

    /**
     * Type of instance
     */
    protected String instanceType;

    /**
     * Method that reads a whitelist
     * @param filename source of whitelist
     * @return whitelist of allowed IDs
     */
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

    /**
     * Method that initializes all sets in isWorking as false (no instances working)
     */
    private void initializeIsWorking() {
        this.isWorking = new HashMap<>();
        for (Integer integer : IDs) {
            isWorking.put(integer, false);
        }
    }

    /**
     * Method that removes an instance from the list of active instances, also removing its interface, address and port
     * from the respective lists
     * @param address instance's address
     * @param port instance's port
     * @param ID instance's id
     * @throws RemoteException If a remote communication error occurs.
     */
    protected abstract void removeInstance(String address, int port, int ID) throws RemoteException;

    /**
     * Class constructor, attributes are initializes
     * @param whitelistPath path to whitelist file
     * @throws RemoteException If a remote communication error occurs.
     */
    protected InstanceManager(String whitelistPath) throws RemoteException {
        this.LOGGER = Logger.getLogger(this.getClass().getName());
        this.addresses = new HashMap<>();
        this.ports = new HashMap<>();
        this.IDs = readWhitelist(whitelistPath);
        this.activeInstances = 0;
        this.addresses = new HashMap<>();
        initializeIsWorking();
    }

    /**
     * Method that returns number of active instances
     * @return number of active instances
     * @throws RemoteException If a remote communication error occurs.
     */
    public int getActiveInstances() throws RemoteException {
        //System.out.println("There are " + this.activeInstances + " active instances");
        return this.activeInstances;
    }

    /**
     * Method that verifies if a given ID is available. If true, adds the instance to all lists
     * @param ID id to verify
     * @param address address of instance
     * @param port port of instance
     * @return true if ID is available
     * @throws RemoteException If a remote communication error occurs.
     */
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

    public int getMaxInstances() throws RemoteException {
        return this.IDs.size();
    }
}
