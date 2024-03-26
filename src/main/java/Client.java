
// TODO -> EXCEPTION MALFORMED URL
// TODO -> DIFERENTES CLIENTES (USAR EXCEPTION)

import java.net.MalformedURLException;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

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

    public void listening() throws RemoteException {
        while (listen) {
            String command = scanner.nextLine();
            String[] tokens = command.split(" ");
            String result;
            //TODO -> tratar de maiúsculas e etc
            switch (tokens[0]) {
                case "exit" ->
                    listen = false;
                case "status" -> {
                    result = this.gateway.status();
                    System.out.println(result);
                }
                case "search" -> {
                    String[] remainingTokens = new String[tokens.length - 1];
                    System.arraycopy(tokens, 1, remainingTokens, 0, tokens.length - 1);
                    // TODO -> receber o número da página do utilizador
                    result = this.gateway.search(remainingTokens, 0);
                    System.out.println(result);
                }
                case "connections" -> {
                    result = this.gateway.getConnections(tokens[1]);
                    System.out.println(result);
                }
                case "insert" -> {
                    result = this.gateway.insert(tokens[1]);
                    System.out.println(result);
                }
                default -> System.out.println("Invalid command \n");
            }
        }
        scanner.close();
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        Client client = new Client("rmi://localhost:1100/gateway");
        client.listening();
    }

}
