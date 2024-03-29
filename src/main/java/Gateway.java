import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;

public class Gateway extends UnicastRemoteObject implements GatewayInterface, Runnable{
    BarrelInterface barrel;
    QueueInterface queue;
    private final String barrelPath;
    private final String queuePath;
    //1100
    public final int PORT;
    public final int barrelNumber;
    public int barrelInUse;
    private long totalDuration = 0;
    private long numSearches = 0;


    public Gateway(int port,int barrelNumber, String barrelPath, String queuePath) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.barrelInUse = 1;
        this.barrelNumber = barrelNumber;
        this.queuePath = queuePath;
        this.barrelPath = barrelPath;
        this.PORT = port;
        LocateRegistry.createRegistry(this.PORT).rebind("gateway", this);
    }
    private void connectToQueue(){
        try{
            this.queue = (QueueInterface) Naming.lookup(queuePath);
        }
        catch(NotBoundException notBoundException){
            System.out.println("[GATEWAY]: Queue not found");
        }
        catch (RemoteException remoteException){
            System.out.println("[GATEWAY]: Remote Exception in Queue");
        }
        catch (MalformedURLException malformedURLException){
            System.out.println("[GATEWAY]: Malformed URL Exception in Queue");
        }
        finally {
            System.out.println("[GATEWAY]: Queue found");
        }
    }

    private void connectToBarrel(){
        try{
            System.out.println("[GATEWAY]: Connecting to barrel number "+ barrelInUse);
            this.barrel = (BarrelInterface) Naming.lookup(barrelPath + barrelInUse);
        }
        catch(NotBoundException notBoundException){
            System.out.println("[GATEWAY]: Barrel number "+ barrelInUse +" not found. Trying next barrel...");
            barrelInUse = (barrelInUse + 1) % (barrelNumber);
            //connectToBarrel();
        }
        catch (RemoteException remoteException){
            System.out.println("[GATEWAY]: Remote Exception");
        }
        catch (MalformedURLException malformedURLException){
            System.out.println("[GATEWAY]: Malformed URL Exception");
        }
        finally {
            System.out.println("[GATEWAY]: Barrel found");
        }
    }
    public void run(){
        connectToQueue();
        connectToBarrel();
        System.out.println("[GATEWAY]: Gateway Ready...");
    }

    // TODO -> verificar interção
    public String search(String[] tokens, int pageNumber) throws RemoteException {
        //TODO -> verificar onde colocar os temporizadores
        long startTime = System.currentTimeMillis();
        System.out.println("[GATEWAY]: Searching for: " + tokens[0]);
        WebPage[] webPages = barrel.search(tokens, 0);
        System.out.println("[GATEWAY]: Search done");
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
        HashSet<Integer> barrelID = new HashSet<>();
        // Get the original status message from the barrel
        String splitRes[] = barrel.status().split("§");

        String topSearches = splitRes[0];
        String activeBarrels = splitRes[1];

        // Calculate average search time
        //TODO -> quando ainda não existem searches
        long averageDuration = totalDuration / numSearches;

        // Format the average duration message
        String averageTimeMessage = "AVERAGE SEARCH TIME: " + averageDuration + " ms.";

        // Combine the original status message and the average time message
        String combinedMessage = topSearches + "\n" + averageTimeMessage + "\n" + activeBarrels + "\n";

        // Return the combined message
        return combinedMessage;
    }
    public void insert(String URL) throws RemoteException {
        System.out.println("[GATEWAY]: Inserting URL: " + URL);
        this.queue.addURL(URL);
    }

    public String getConnections(String URL) throws RemoteException {
        String result = "Resultado: \n";
        result = result.concat(barrel.getConnections(URL));

        System.out.println(result);

        return result;
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        GatewayInterface gateway = new Gateway(1100, 1,"rmi://localhost:4321/barrel1", "rmi://localhost/queue");

        System.out.println("[GATEWAY]: Gateway Ready...");
    }



}
