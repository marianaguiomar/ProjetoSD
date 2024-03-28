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

    private long totalDuration = 0;
    private long numSearches = 0;

    public Gateway(String barrelPath, String queuePath) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.queue = (QueueInterface) Naming.lookup(queuePath);
        this.PORT = 1100;
        this.barrel = (BarrelInterface) Naming.lookup(barrelPath);
    }

    // TODO -> verificar interção
    public String search(String[] tokens, int pageNumber) throws RemoteException {
        //TODO -> verificar onde colocar os temporizadores
        long startTime = System.currentTimeMillis();
        WebPage[] webPages = barrel.search(tokens, 0);
        long endTime = System.currentTimeMillis();
        long duration = endTime-startTime;
        totalDuration += duration;
        numSearches += 1;

        StringBuilder result= new StringBuilder("RESULTADOS PARA A PÁGINA " + pageNumber + "\n");
        for (WebPage webPage : webPages) {
            result.append(webPage.toString()).append("\n");
        }
        return result.toString();
    }
    public String status() throws RemoteException {
        // Get the original status message from the barrel
        String topSearches = barrel.status();

        // Calculate average search time
        long averageDuration = totalDuration / numSearches;

        // Format the average duration message
        String averageTimeMessage = "AVERAGE TIME: " + averageDuration + " ms.";

        // Combine the original status message and the average time message
        String combinedMessage = topSearches + "\n" + averageTimeMessage + "\n";

        // Return the combined message
        return combinedMessage;
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
