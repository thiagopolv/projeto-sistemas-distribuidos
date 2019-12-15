package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.AuctionServiceGrpc;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

import java.util.*;

import static util.ConfigProperties.*;

public class AuctionClient {

    static final String SEPARATOR =
            "_______________________________________________________________________________";


    private static final String DISCONNECT_VALUE = ClientAction.DISCONNECT.toString();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final Integer SERVER_PORT = getServerPort();

    private List<Integer> getServerPorts() {
        List<Integer> serverPorts = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            serverPorts.add(SERVER_PORT + i);
        }

        return serverPorts;
    }


    private ManagedChannel buildChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private void chooseAction(ClientConnectionProperties connectionProperties) {
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
            executeAction(connectionProperties, action.toUpperCase(), username);

        } while (!action.toUpperCase().equals(DISCONNECT_VALUE));
    }

    private void executeAction(ClientConnectionProperties connectionProperties, String action, String username) {

        ClientAction clientAction = Arrays.stream(ClientAction.values())
                .filter(p -> p.toString().equals(action.toUpperCase()))
                .findFirst().orElse(ClientAction.INVALID);

        ClientService clientService = new ClientService(connectionProperties, username);

        clientService.resolveClientAction(clientAction);
    }

    private Integer getRandomPort(List<Integer> portList) {
        return portList.get(new Random().nextInt(portList.size()));
    }

    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        List<Integer> ports = client.getServerPorts();
        Integer port = client.getRandomPort(ports);
        System.out.println("Connecting on port: " + port);
        ManagedChannel channel = client.buildChannel(getServerHost(), port);
        AuctionServiceBlockingStub stub = AuctionServiceGrpc.newBlockingStub(channel);

        client.chooseAction(new ClientConnectionProperties(stub, port, channel));

        channel.shutdown();
    }
}
