package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.AuctionServiceGrpc;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

import java.util.*;

import static util.ConfigProperties.*;

public class AuctionClient {

    private static final String SEPARATOR =
            "_______________________________________________________________________________";

    private static final String DISCONNECT_VALUE = ClientAction.DISCONNECT.toString();

    private void chooseAction() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nCONNECTION " + "\n");
        System.out.println("Username: ");
        String username = scanner.nextLine();
        System.out.println("Password: ");
        @SuppressWarnings("unused") String password = scanner.nextLine();
        String action;

        do {
            System.out.println("==================================================");
            System.out.println("Choose an action:");
            System.out.println("1 - LIST auctions;");
            System.out.println("2 - SEND bid;");
            System.out.println("3 - CREATE auction;");
            System.out.println("4 - DISCONNECT. ");
            System.out.println("==================================================");
            System.out.println("Action: ");
            action = scanner.nextLine();
            System.out.println(SEPARATOR);
            executeAction(action.toUpperCase(), username);

        } while (!action.toUpperCase().equals(DISCONNECT_VALUE));
    }

    private void executeAction(String action, String username) {

        ClientAction clientAction = Arrays.stream(ClientAction.values())
                .filter(p -> p.toString().equals(action.toUpperCase()))
                .findFirst().orElse(ClientAction.INVALID);

        ClientService clientService = new ClientService(username);

        clientService.resolveClientAction(clientAction);
    }

    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        client.chooseAction();
    }
}
