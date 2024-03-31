import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
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

    private HashMap<Integer,Long> totalDuration;
    private HashMap<Integer, Integer> numSearches;


    public Gateway(int port,int barrelNumber, String barrelPath, String queuePath) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.barrelInUse = 2;
        this.barrelNumber = barrelNumber;
        this.queuePath = queuePath;
        this.barrelPath = barrelPath;
        this.PORT = port;
        this.totalDuration = new HashMap<>();
        this.numSearches = new HashMap<>();
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
    public void updateSearches(long duration){
        if (!totalDuration.containsKey(barrelInUse)) {
            totalDuration.put(barrelInUse, duration);
        }
        else {
            long currDuration = totalDuration.get(barrelInUse);
            long updatedDuration = currDuration + duration;
            totalDuration.put(barrelInUse, updatedDuration);
        }

        if (!numSearches.containsKey(barrelInUse)) {
            numSearches.put(barrelInUse, 1);
        }
        else {
            long currSearches = numSearches.get(barrelInUse);
            long updatedSearches = currSearches + 1;
            totalDuration.put(barrelInUse, updatedSearches);
        }
    }
    public String searchIntersection(String[] tokens, int pageNumber) throws RemoteException {
        //TODO -> verificar onde colocar os temporizadores
        long startTime = System.currentTimeMillis();
        System.out.println("[GATEWAY]: Searching for: " + tokens[0]);
        WebPage[] webPages = barrel.searchIntersection(tokens, pageNumber - 1);
        System.out.println("[GATEWAY]: Search done");

        long endTime = System.currentTimeMillis();
        updateSearches(endTime-startTime);

        StringBuilder result= new StringBuilder(webPages.length + " RESULTADOS PARA A PÁGINA " + pageNumber + "\n");
        for (WebPage webPage : webPages) {
            result.append(webPage.toString()).append("\n");
        }
        return result.toString();
    }

    public String searchUnion(String[] tokens, int pageNumber) throws RemoteException {
        //TODO -> verificar onde colocar os temporizadores
        long startTime = System.currentTimeMillis();
        System.out.println("[GATEWAY]: Searching for: " + tokens[0]);
        WebPage[] webPages = barrel.searchUnion(tokens, pageNumber - 1);
        System.out.println("[GATEWAY]: Search done");

        long endTime = System.currentTimeMillis();
        updateSearches(endTime-startTime);

        StringBuilder result= new StringBuilder(webPages.length +" RESULTADOS PARA A PÁGINA " + pageNumber + "\n");
        for (WebPage webPage : webPages) {
            result.append(webPage.toString()).append("\n");
        }
        return result.toString();
    }
    //TODO -> fix
    public String status() throws RemoteException {
        HashSet<Integer> barrelID = new HashSet<>();
        // Get the original status message from the barrel
        String splitRes[] = barrel.status().split("§");

        String topSearches = splitRes[0];
        String activeBarrels = splitRes[1];

        // Calculate average search time
        String averageTimeMessage = formatAverageTime();

        // Combine the original status message and the average time message
        String combinedMessage = topSearches + "\n" + averageTimeMessage + "\n" + activeBarrels + "\n";

        // Return the combined message
        return combinedMessage;
    }
    public void insert(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        System.out.println("[GATEWAY]: Inserting URL: " + URL);
        this.queue.addURL(URL);
    }

    public String formatAverageTime() {
        StringBuilder result = new StringBuilder();
        result.append("AVERAGE SEARCH TIME: \n");
        //TODO -> quando ainda não existem searches -> verificar se funciona, outro barrel n faz nada
        for (Integer barrel: totalDuration.keySet()) {
            long averageDuration = totalDuration.get(barrel) / numSearches.get(barrel);
            result.append("BARREL#").append(barrel).append(": ").append(averageDuration).append(" ms.");
        }

        String res = result.toString();
        return res;
    }

    public String getConnections(String URL) throws RemoteException {
        URL = URL.toLowerCase();
        String result = "Resultado: \n";
        System.out.println("[GATEWAY]: Getting connections for URL: " + URL);
        result = result.concat(barrel.getConnections(URL));

        System.out.println(result);

        return result;
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        GatewayInterface gateway = new Gateway(1100, 1,"rmi://localhost:4321/barrel1", "rmi://localhost/queue");

        System.out.println("[GATEWAY]: Gateway Ready...");
    }



}
