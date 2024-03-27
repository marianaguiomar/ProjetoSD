import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;

public class Gateway extends UnicastRemoteObject implements GatewayInterface{
    BarrelInterface barrel;
    QueueInterface queue;
    public final int PORT;
    public Gateway(String barrelPath, String queuePath) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.PORT = 1100;
        this.barrel = (BarrelInterface) Naming.lookup(barrelPath);
    }

    // TODO -> verificar interção
    public String search(String[] tokens, int pageNumber) throws RemoteException {
        WebPage[] webPages = barrel.search(tokens, 0);
        StringBuilder result= new StringBuilder("RESULTADOS PARA A PÁGINA " + pageNumber + "\n");
        for (WebPage webPage : webPages) {
            result.append(webPage.toString()).append("\n");
        }
        return result.toString();
    }
    public String status() throws RemoteException {
        return "";
    }
    public void insert(String URL) throws RemoteException {
        this.queue.addURL(URL);
    }
    public String getConnections(String URL) throws RemoteException {
        String result = "Resultado: \n";
        result = result.concat(barrel.getConnections(URL));

        System.out.println(result);

        return result;
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        GatewayInterface gateway = new Gateway("rmi://localhost:4321/barrel", "rmi://localhost/queue");
        LocateRegistry.createRegistry(1100).rebind("gateway", gateway);
        System.out.println("Gateway Ready...");
    }



}
