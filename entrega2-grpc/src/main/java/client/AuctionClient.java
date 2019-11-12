package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.Auction;
import server.AuctionServiceGrpc;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.CreateAuctionRequest;
import server.CreateAuctionResponse;
import server.CurrentBidInfo;
import server.GetAuctionsRequest;
import server.GetAuctionsResponse;
import server.SendBidRequest;
import server.SendBidResponse;

import static client.ClientAction.INVALID;
import static client.ClientAction.getEnumMap;
import static java.lang.Boolean.FALSE;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static util.ConfigProperties.getDaysToExpireAuction;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AuctionClient {

    private static final String SEND_BID_SUCCESS_MESSAGE = "Your bid was sent successfully.\n\n";
    private static final String SEND_BID_FAIL_MESSAGE = "There was an error sending your bid. Please, refresh " +
            "the auction list and try again.\n\n";
    private static final String INVALID_BID = "Your bid is less than the current bid: ";
    private static final String DISCONNECT_VALUE = ClientAction.DISCONNECT.getAction();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final Integer SERVER_PORT = getServerPort();
    private static final Integer DAYS_TO_FINISH_AUCTION = getDaysToExpireAuction();

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
        System.out.println("1 - LIST auctions;");
        System.out.println("2 - SEND bid;");
        System.out.println("3 - CREATE auction;");
        System.out.println("4 - DISConnect. ");
        System.out.println("==================================================");

        do {
            System.out.println("Action: ");
            action = scanner.nextLine();
            System.out.println("_______________________________________________________________________________");
            executeAction(action.toUpperCase(), stub, username, port);

        } while (!action.toUpperCase().equals(DISCONNECT_VALUE));
    }


    private void executeAction(String action, AuctionServiceBlockingStub stub, String username, Integer port) {

        ClientAction clientAction = getEnumMap().containsValue(action) ? getEnumMap().getKey(action) : INVALID;

        switch (clientAction) {
            case LIST:
                printAuctions(getAuctions(stub, port).getAuctionsList());
                break;
            case SEND_BID:
                List<Auction> auctions = getAuctions(stub, port).getAuctionsList();
                sendBidAndReturnMessage(stub, auctions, username, port);
                break;
            case CREATE:
                CreateAuctionResponse response = stub.createAuction(buildCreateAuctionRequestFromUserInput(username,
                    port));
                sendCreateAuctionMessage(response);
                break;
            case DISCONNECT:
                sendDisconnectMessage();
                break;
            case INVALID:
                sendInvalidActionMessage();
        }
    }

    private void sendCreateAuctionMessage(CreateAuctionResponse createAuctionResponse) {

        if(createAuctionResponse.getSuccess()) {
            System.out.println("Auction was created successfully.");
        } else {
            System.out.println("An error ocurred creating the auction.");
        }
        System.out.println("_______________________________________________________________________________");
    }

    private void sendDisconnectMessage() {
        System.out.println("Disconnected.");
    }

    private void sendInvalidActionMessage() {
        System.out.println("Invalid action, please send another.");
        System.out.println("_______________________________________________________________________________");
    }

    private CreateAuctionRequest buildCreateAuctionRequestFromUserInput(String username, Integer port) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the product you want to auction:");
        String product = sc.nextLine();
        System.out.println("Enter the initial value:");
        Double initialValue = sc.nextDouble();

        Auction auction = buildAuction(username, product, initialValue);

        return buildCreateAuctionRequest(auction, port);
    }

    private CreateAuctionRequest buildCreateAuctionRequest(Auction auction, Integer port) {
        return CreateAuctionRequest.newBuilder()
                .setPort(port)
                .setIsServer(FALSE)
                .setAuction(auction)
                .build();
    }

    private Auction buildAuction(String username, String product, Double initialValue) {
        return Auction.newBuilder()
                .setProduct(product)
                .setInitialValue(initialValue)
                .setFinishTime(LocalDateTime.now().plusDays(DAYS_TO_FINISH_AUCTION).toString())
                .setCurrentBidInfo(buildCurrentBidInfo(initialValue))
                .setOwner(username)
                .build();
    }

    private CurrentBidInfo buildCurrentBidInfo(Double initialValue) {
        return CurrentBidInfo.newBuilder()
                .setUsername("")
                .setValue(initialValue)
                .build();
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

    private void sendBidAndReturnMessage(AuctionServiceBlockingStub stub, List<Auction> auctions, String username,
            Integer port) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Insert the auction Id you want to send the bid: ");
        Integer id = sc.nextInt();
        System.out.println("Insert bid value: (Format: 10.00) ");
        Double bid = sc.nextDouble();
        System.out.println("_______________________________________________________________________________");

        Double currentBid = getAuctionById(auctions, id).getCurrentBidInfo().getValue();

        if (isInvalidBid(bid, currentBid)) {
            System.out.println(INVALID_BID + String.format("%.2f", currentBid));
            System.out.println("_______________________________________________________________________________");
        } else {
            SendBidResponse sendBidResponse = stub.sendBid(buildSendBidRequest(id, bid, username, port));
            System.out.println(sendBidResponse.getSuccess() ? SEND_BID_SUCCESS_MESSAGE : SEND_BID_FAIL_MESSAGE);
            System.out.println("_______________________________________________________________________________");
        }
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
        client.chooseAction(stub, port);
        channel.shutdown();
    }
}
