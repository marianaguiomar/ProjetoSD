import Googol.Gateway.GatewayInterface;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {

    Scanner scanner;

    boolean listen = true;

    GatewayInterface gateway;
    public Client(String gatewayPath) throws MalformedURLException, RemoteException, NotBoundException {
        this.scanner = new Scanner(System.in);
        this.gateway = (GatewayInterface) Naming.lookup(gatewayPath);
    }
    private boolean retryConnection(){
        int conectAttempts = 0;
        boolean connected = false;
        while (conectAttempts < 5 && !connected) {
            try {
                this.gateway = (GatewayInterface) Naming.lookup("rmi://localhost:1100/gateway");
                System.out.println("[CLIENT]: Gateway reconnected");
                return true;
            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                conectAttempts++;
                System.out.println("[CLIENT]: Gateway is not available trying to reconnect");
            }
        }
        return false;
    }
    public void listening(){
        try {
            while (listen) {
                String command = scanner.nextLine();
                String[] tokens = command.split(" ");
                String result;
                switch (tokens[0]) {
                    case "exit" -> listen = false;
                    case "status" -> {
                        result = this.gateway.status();
                        System.out.println(result);
                    }
                    case "search" -> {
                        if (tokens.length < 4) {
                            System.out.println("[CLIENT]: Invalid command \n");
                            break;
                        }
                        String type = tokens[1];
                        try {
                            int pageNumber = Integer.parseInt(tokens[2]);
                            if (pageNumber < 1) {
                                System.out.println("[CLIENT]: Invalid command \n");
                                break;
                            }
                            String[] remainingTokens = new String[tokens.length - 3];
                            System.arraycopy(tokens, 3, remainingTokens, 0, tokens.length - 3);
                            if (type.equals("i")) {
                                result = this.gateway.search(remainingTokens, pageNumber, true);
                            } else if (type.equals("u")) {
                                result = this.gateway.search(remainingTokens, pageNumber, false);
                            } else {
                                System.out.println("[CLIENT]: Invalid command \n");
                                break;
                            }
                            System.out.println(result);
                        } catch (NumberFormatException e) {
                            System.out.println("[CLIENT]: Invalid command \n");
                        }

                    }
                    case "connections" -> {
                        result = this.gateway.getConnections(tokens[1]);
                        System.out.println(result);
                    }
                    case "insert" -> {
                        this.gateway.insert(tokens[1]);
                        System.out.println("[CLIENT]: Link inserted\n");
                    }
                    default -> System.out.println("[CLIENT]: Invalid command \n");
                }
            }
            scanner.close();
        }
        catch (RemoteException e) {
            System.out.println("[CLIENT]: Gateway is not available trying to reconnect");
            if(retryConnection())
                listening();
        }
        catch (NullPointerException e){
            System.out.println("[CLIENT]: Failed to retrieve results from the gateway");
            if(retryConnection())
                listening();
        }
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        if (args.length != 2) {
            System.out.println("Usage: java Client <gatewayIP> <gatewayPort>");
            System.exit(1);
        }
        String gatewayAddress = "rmi://" + args[0] + ":" + args[1] + "/gateway";
        Client client = new Client(gatewayAddress);
        client.listening();
    }

}
