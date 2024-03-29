import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ProjectManager {
    private final String MULTICAST_ADDRESS = "224.3.2.1";
    private final int PORT = 4321;

    Registry registry;
    final int gatewayPort;
    final int numberOfDownloaders;
    final int numberOfBarrels;
    Thread gatewayThread;
    Thread[] barrelsThreads;
    Thread[] downloadersThreads;

    private void initializeBarrels() {
        barrelsThreads = new Thread[numberOfBarrels];
        for (int i = 1; i <= numberOfBarrels; i++) {
            try {
                Barrel barrel = new Barrel(registry, MULTICAST_ADDRESS, PORT, i);
                barrelsThreads[i - 1] = new Thread(barrel);
                barrelsThreads[i - 1].start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeDownloaders() {
        downloadersThreads = new Thread[numberOfDownloaders];
        for (int i = 0; i < numberOfDownloaders; i++) {
            try {
                Downloader downloader = new Downloader(MULTICAST_ADDRESS, PORT,
                        "rmi://localhost:" + this.PORT + "/queue", i+1);
                downloadersThreads[i] = new Thread(downloader);
                downloadersThreads[i].start();
            } catch (IOException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
    private void initializeGateway() {
        try {
            Gateway gateway = new Gateway(gatewayPort, numberOfBarrels, "rmi://localhost:"+ PORT +"/barrel",
                    "rmi://localhost:" + this.PORT + "/queue");
            gatewayThread = new Thread(gateway);
            gatewayThread.start();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ProjectManager(int gatewayPort, int numberOfDownloaders, int numberOfBarrels) throws RemoteException {
        this.gatewayPort = gatewayPort;
        this.numberOfDownloaders = numberOfDownloaders;
        this.numberOfBarrels = numberOfBarrels;
        try {
            // Create RMI registry
            registry = LocateRegistry.createRegistry(PORT);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Queue queue = new Queue(registry);
        initializeDownloaders();
        initializeBarrels();
        initializeGateway();

    }





    public static void main(String[] args) throws RemoteException {
        ProjectManager projectManager = new ProjectManager(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                Integer.parseInt(args[2]));
    }
}
