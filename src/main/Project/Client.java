// TODO -> EXCEPTION MALFORMED URL
// TODO -> DIFERENTES CLIENTES (USAR EXCEPTION)

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
        // TODO -> verificar se se faz isto aqui
        this.scanner = new Scanner(System.in);
        this.gateway = (GatewayInterface) Naming.lookup(gatewayPath);
    }
    private void retryConnection(){
        int conectAttempts = 0;
        boolean connected = false;
        while (conectAttempts < 5 && !connected) {
            try {
                this.gateway = (GatewayInterface) Naming.lookup("rmi://localhost:1100/gateway");
                System.out.println("[CLIENT]: Gateway reconnected");
                connected = true;
            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                conectAttempts++;
                System.out.println("[CLIENT]: Gateway is not available trying to reconnect");
            }
        }
    }
    public void listening(){
        try {
            while (listen) {
                String command = scanner.nextLine();
                String[] tokens = command.split(" ");
                String result;
                //TODO -> tratar de maiúsculas e etc
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
            retryConnection();
        }
        catch (NullPointerException e){
            System.out.println("[CLIENT]: Failed to retrieve results from the gateway");
        }
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        Client client = new Client("rmi://localhost:1100/gateway");
        client.listening();
    }

}
