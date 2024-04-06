package Googol.Client;

import Googol.Gateway.GatewayInterface;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

/**
 * Class that manages the client
 */
public class Client {

    /**
     * Scanner to receive input
     */
    private final Scanner scanner;

    /**
     * Welcome message
     */
    String welcomeMessage = """
        **************************************************************
        *                【Welcome to the Server】                    *
        *                                                            *
        *  Available commands:                                       *
        *  ➜ exit:   To exit the server                              *
        *  ➜ status: To get the status                               *
        *  ➜ search [type] [pageNumber] [query]: To search for items *
        *       ➜[type] can be 'i' for intersection or 'u' for union *
        *  ➜ connections [parameter]: To get connections information *
        *  ➜ insert [link]: To insert a link                         *
        *                                                            *
        *      ❦ Made by Saulo José Mendes and Mariana Guiomar ❦     *
        **************************************************************
        """;

    /**
     * True if the client process is listening for input
     */
    private boolean listen = true;

    /**
     * Gateway address
     */
    private final String gatewayAddress;

    /**
     * Gateway inteface
     */
    private GatewayInterface gateway;

    /**
     * Class constructor, attributes are initialized
     * @param gatewayAddress Gateway address
     * @throws MalformedURLException If the URL is invalid
     * @throws RemoteException If a remote communication error occurs.
     * @throws NotBoundException If the object is not bound
     */
    public Client(String gatewayAddress) throws MalformedURLException, RemoteException, NotBoundException {
        this.scanner = new Scanner(System.in);
        this.gatewayAddress = gatewayAddress;
        this.gateway = (GatewayInterface) Naming.lookup(gatewayAddress);
        System.out.print(welcomeMessage);
    }

    /**
     * Method that retries connection to Gateway a maximum of 5 times
     * @return wether the reconnection was successful
     */
    private boolean retryConnection(){
        int conectAttempts = 0;
        boolean connected = false;
        while (conectAttempts < 5 && !connected) {
            try {
                this.gateway = (GatewayInterface) Naming.lookup(this.gatewayAddress);
                System.out.println("[CLIENT]: Gateway reconnected");
                return true;
            } catch (RemoteException | NotBoundException | MalformedURLException e) {
                conectAttempts++;
                System.out.println("[CLIENT]: Gateway is not available trying to reconnect");
            }
        }
        return false;
    }

    /**
     * Method that performs Googol.Client.Client's operations while it's running
     */
    public void listening(){
        try {
            while (listen) {
                System.out.println("[CLIENT]: Please input your commands to interact with the server.");
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
                        if (tokens.length != 2) {
                            System.out.println("[CLIENT]: Invalid command \n");
                            break;
                        }
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
            listening();
        }
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        if (args.length != 2) {
            System.out.println("Usage: java Googol.Client.Client <gatewayIP> <gatewayPort>");
            System.exit(1);
        }
        String gatewayAddress = "rmi://" + args[0] + ":" + args[1] + "/gateway";
        Client client = new Client(gatewayAddress);
        client.listening();
    }

}
