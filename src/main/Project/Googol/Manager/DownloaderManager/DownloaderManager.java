package Googol.Manager.DownloaderManager;

import Googol.Manager.InstanceManager;

import java.rmi.RemoteException;

public class DownloaderManager extends InstanceManager implements DownloaderManagerInterface  {
    public DownloaderManager(String whitelistPath) throws RemoteException {
        super(whitelistPath);
        this.instanceType = "[DOWNLOADERMANAGER#]";
    }

    public void removeInstance(String address, int port, int ID) throws RemoteException {
        System.out.println(this.instanceType + ": Removing DOWNLOADER#" + ID);
        activeInstances--;
        isWorking.put(ID, false);
        this.addresses.remove(ID);
        this.ports.remove(ID);
    }
}


