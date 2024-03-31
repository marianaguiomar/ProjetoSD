import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectManager {
    private final String MULTICAST_ADDRESS = "224.3.2.1";
    private final int PORT = 4321;
    private final String CONFIRMATION_MULTICAST_ADDRESS = "224.3.2.2";
    private final int CONFIRMATION_PORT = 4322;
    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

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
                Barrel barrel = new Barrel(registry, MULTICAST_ADDRESS, PORT, CONFIRMATION_MULTICAST_ADDRESS, CONFIRMATION_PORT,i);
                barrelsThreads[i - 1] = new Thread(barrel);
                barrelsThreads[i - 1].start();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
            }
        }
    }

    private void initializeDownloaders() {
        downloadersThreads = new Thread[numberOfDownloaders];
        for (int i = 0; i < numberOfDownloaders; i++) {
            try {
                Downloader downloader = new Downloader(MULTICAST_ADDRESS, PORT, CONFIRMATION_MULTICAST_ADDRESS, CONFIRMATION_PORT,
                        "rmi://localhost:" + this.PORT + "/queue", i+1);
                downloadersThreads[i] = new Thread(downloader);
                downloadersThreads[i].start();
            } catch (IOException | NotBoundException e) {
                LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
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
            LOGGER.log(Level.SEVERE, "Remote exception occurred"+ e.getMessage(), e);
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
