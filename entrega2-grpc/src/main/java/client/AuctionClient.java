package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.Auction;
import server.AuctionServiceGrpc;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.GetAuctionsRequest;
import server.GetAuctionsResponse;
import server.SendBidRequest;
import server.SendBidResponse;

import static client.ClientAction.INVALID;
import static client.ClientAction.getEnumMap;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AuctionClient {

    //    private static final String ROOT_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath();
//    private static final String CONFIG_PATH = ROOT_PATH + "application.properties";
    private static final String SEND_BID_SUCCESS_MESSAGE = "Your bid was sent successfully.\n\n";
    private static final String SEND_BID_FAIL_MESSAGE = "There was an error sending your bid. Please, refresh " +
            "the auction list and try again.\n\n";
    private static final String INVALID_BID = "Your bid is less than the current bid: ";
    private static final String DISCONNECT = "DIS";
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final Integer SERVER_PORT = getServerPort();


    private List<Integer> getServerPorts() {
        List<Integer> serverPorts = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            serverPorts.add(SERVER_PORT + i);
        }

        return serverPorts;
    }

    private GetAuctionsRequest buildGetAuctionsRequest(Integer port) {
        return GetAuctionsRequest.newBuilder().setPort(port).build();
    }

    public GetAuctionsResponse getAuctions(AuctionServiceBlockingStub stub, Integer port) {
        return stub.getAuctions(buildGetAuctionsRequest(port));
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public void chooseAction(AuctionServiceBlockingStub stub, Integer port) throws IOException, InterruptedException {
        String action = EMPTY;
        String username = EMPTY;
        Scanner scanner = new Scanner(System.in);

        System.out.println("\nCONNECTION " + "\n");
        System.out.println("Username: ");
        username = scanner.nextLine();
        System.out.println("Password: ");
        scanner.nextLine();

        System.out.println("==================================================");
        System.out.println("Choose an action:");
        System.out.println("1 - LIST auctions; / 2 - SEND bid; / ");
        System.out.println("3 - DISconnect.");
        System.out.println("==================================================");

        do {
            System.out.println("Action: ");
            action = scanner.nextLine();
            System.out.println("_______________________________________________________________________________");
            executeAction(action.toUpperCase(), stub, username, port);

        } while (!action.toUpperCase().equals(DISCONNECT));
    }


    private void executeAction(String action, AuctionServiceBlockingStub stub, String username, Integer port) {
        ClientAction clientAction = getEnumMap().containsValue(action) ? getEnumMap().getKey(action) : INVALID;

        switch (clientAction) {
            case LIST:
                printAuctions(getAuctions(stub, port).getAuctionsList());
                break;
            case SEND_BID:
                List<Auction> auctions = getAuctions(stub, port).getAuctionsList();
                System.out.println(sendBidAndReturnMessage(stub,
                        auctions, username, port) + "\n\n");
                break;
            case DISCONNECT:
                System.out.println("Disconnected.");
                break;
            case INVALID:
                System.out.println("Invalid action, please send another.");
                System.out.println("_______________________________________________________________________________");
        }
    }

    private void printAuctions(List<Auction> auctions) {
        auctions.stream()
            .sorted(comparingInt(Auction::getId))
            .forEach(auction -> {
                System.out.println("ID: " + auction.getId() + "   PRODUCT: " + auction.getProduct()
                        + "    OWNER: " + auction.getOwner());
                System.out.println("INITIAL VALUE: " + auction.getInitialValue()
                        + "    FINISH DATE: " + auction.getFinishTime());
                System.out.println("CURRENT BID: " + auction.getCurrentBidInfo().getValue()
                        + "    BUYER: " + auction.getCurrentBidInfo().getUsername());
                System.out.println("_______________________________________________________________________________");
        });
    }

    private String sendBidAndReturnMessage(AuctionServiceBlockingStub stub, List<Auction> auctions, String username,
            Integer port) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Insert the auction Id you want to send the bid: ");
        Integer id = sc.nextInt();
        System.out.println("Insert bid value: (Format: 10.00) ");
        Double bid = sc.nextDouble();
        System.out.println("_______________________________________________________________________________");

        Double currentBid = getAuctionById(auctions, id).getCurrentBidInfo().getValue();

        if (isInvalidBid(bid, currentBid)) {
            return INVALID_BID + String.format("%.2f", currentBid);
        }

        SendBidResponse sendBidResponse = stub.sendBid(buildSendBidRequest(id, bid, username, port));

        return sendBidResponse.getSuccess() ? SEND_BID_SUCCESS_MESSAGE : SEND_BID_FAIL_MESSAGE;
    }

    private boolean isInvalidBid(Double bid, Double currentBid) {
        return bid < currentBid;
    }


    private Auction getAuctionById(List<Auction> list, Integer id) {

        return list.stream()
                .filter(auction -> auction.getId() == id)
                .collect(Collectors.toList())
                .get(0);
    }

    private SendBidRequest buildSendBidRequest(Integer id, Double bid, String username, Integer port) {
        return SendBidRequest.newBuilder()
                .setId(id)
                .setBid(bid)
                .setUsername(username)
                .setPort(port)
                .build();
    }

    private Integer getRandomPort(List<Integer> portList) {
        return portList.get(new Random().nextInt(portList.size()));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        AuctionClient client = new AuctionClient();
        List<Integer> ports = client.getServerPorts();
        Integer port = client.getRandomPort(ports);

        ManagedChannel channel = client.buildChannel(getServerHost(), port);
        AuctionServiceBlockingStub stub = AuctionServiceGrpc.newBlockingStub(channel);
//        SendBidResponse response = stub.sendBid(SendBidRequest
//                .newBuilder()
//                .setIsServer(FALSE)
//                .setPort(50002)
//                .setId(1)
//                .setBid(2)
//                .build());

//        System.out.println(response);
        client.chooseAction(stub, port);
//        channel.shutdown();
    }
}
