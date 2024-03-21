import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;


public class Gateway extends UnicastRemoteObject implements GatewayInterface{
    public Gateway() throws RemoteException {
        super();
    }

    public String search(String[] tokens) throws RemoteException {
        return "";
    }
    public String status() throws RemoteException {
        return "";
    }
    public String insert(String URL) throws RemoteException {
        return "";
    }
    public String getConnections(String URL) throws RemoteException {
        return "";
    }

    public static void main(String[] args) throws RemoteException {
        GatewayInterface gateway = new Gateway();
        LocateRegistry.createRegistry(1100).rebind("gateway", gateway);
        System.out.println("Gateway Ready...");
    }



}
