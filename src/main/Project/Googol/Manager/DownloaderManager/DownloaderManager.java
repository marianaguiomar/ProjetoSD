package Googol.Manager.DownloaderManager;

import Googol.Manager.InstanceManager;

import java.rmi.RemoteException;

/**
 * Class that manages all Downloader
 */
public class DownloaderManager extends InstanceManager implements DownloaderManagerInterface  {
    /**
     * Class constructor, initializes all attributes
     * @param whitelistPath path to the downloader's whitelist
     */
    public DownloaderManager(String whitelistPath) throws RemoteException {
        super(whitelistPath);
        this.instanceType = "[DOWNLOADERMANAGER#]";
    }

    /**
     * Method that removes a downloader from the list of active downloader, also removing its interface, address and port
     * from the respective lists
     * @param address downloader's address
     * @param port downloader's port
     * @param ID downloader's id
     */
    public void removeInstance(String address, int port, int ID) throws RemoteException {
        System.out.println(this.instanceType + ": Removing DOWNLOADER#" + ID);
        activeInstances--;
        isWorking.put(ID, false);
        this.addresses.remove(ID);
        this.ports.remove(ID);
    }
}


