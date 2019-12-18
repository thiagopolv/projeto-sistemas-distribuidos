package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import server.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static util.ConfigProperties.*;

class ClientService {

    private String username;
    private ClientConnectionProperties connectionProperties;

    private static final String SEPARATOR2 =
            "_______________________________________________________________________________\n\n\n";
    private static final String SEND_BID_SUCCESS_MESSAGE = "Your bid was sent successfully.\n\n";
    private static final String SEND_BID_FAIL_MESSAGE = "There was an error sending your bid. Please, refresh " +
            "the auction list and try again.\n\n";
    private static final String INVALID_BID = "Your bid is less than the current bid: ";
    private static final Integer DAYS_TO_FINISH_AUCTION = getDaysToExpireAuction();
    private static final Integer NUMBER_OF_NODES = getNumberOfNodes();
    private static final Integer NUMBER_OF_REPLICAS = getNumberOfReplicas();
    private static final Integer NODE_PORT_DIFFERENCE = getNodePortDifference();
    private static final Integer BASE_PORT = getBasePort();

    ClientService(String username) {
        this.username = username;
        this.connectionProperties = getConnectionProperties();
    }

    private ClientConnectionProperties getConnectionProperties() {
        List<List<Integer>> nodeListPorts = getServerPorts();
        int node = getRandomNode(nodeListPorts.size());
        return new ClientConnectionProperties(nodeListPorts.get(node));
    }

    void resolveClientAction(ClientAction clientAction) {
        clientAction.clientActionFunction.apply(this);
    }

    Void printAuctions() {
        retryable();
        List<Auction> auctions = getAuctionsList();
        if (auctions.isEmpty()) {
            System.out.println("No auctions detected");
            return null;
        }
        auctions.stream()
                .sorted(comparing(Auction::getId))
                .forEach(auction -> {
                    System.out.println("ID: " + auction.getId() + "   PRODUCT: " + auction.getProduct()
                            + "    OWNER: " + auction.getOwner());
                    System.out.println("INITIAL VALUE: " + auction.getInitialValue()
                            + "    FINISH DATE: " + auction.getFinishTime());
                    System.out.println("CURRENT BID: " + auction.getCurrentBidInfo().getValue()
                            + "    BUYER: " + auction.getCurrentBidInfo().getUsername());
                    System.out.println(SEPARATOR2);
                });
        return null;
    }

    Void sendBidAndReturnMessage() {
        retryable();
        List<Auction> auctions = getAuctionsList();

        Scanner sc = new Scanner(System.in);

        System.out.println("Insert the auction Id you want to send the bid: ");
        String id = sc.nextLine();

        Auction auction = getAuctionById(auctions, id);
        if (isNull(auction)) {
            System.out.println("There is no active auction with this ID.");
            return null;
        }

        System.out.println("Insert bid value: (Format: 10.00) ");
        Double bid = sc.nextDouble();
        System.out.println(SEPARATOR2);

        Double currentBid = auction.getCurrentBidInfo().getValue();

        if (isInvalidBid(bid, currentBid)) {
            System.out.println(INVALID_BID + String.format("%.2f", currentBid));
            System.out.println(SEPARATOR2);
        } else {
            SendBidResponse sendBidResponse = connectionProperties.getStub().sendBid(buildSendBidRequest(id, bid, username));
            System.out.println(sendBidResponse.getSuccess() ? SEND_BID_SUCCESS_MESSAGE : SEND_BID_FAIL_MESSAGE);
            System.out.println(SEPARATOR2);
        }
        return null;
    }

    Void sendDisconnectAndSendMessage() {
        System.out.println("Disconnected.");
        connectionProperties.getChannel().shutdown();
        return null;
    }

    Void sendInvalidActionMessage() {
        System.out.println("Invalid action, please send another.");
        System.out.println(SEPARATOR2);
        return null;
    }

    private GetAuctionsResponse getAuctions() {
        GetAuctionsResponse response = null;
        try {
            response = connectionProperties.getStub().getAuctions(buildGetAuctionsRequest());
        } catch (StatusRuntimeException e) {
            retryable();
            this.getAuctions();
        }
        return response;
    }

    private List<Auction> getAuctionsList() {
        return getAuctions().getAuctionsList();
    }

    private GetAuctionsRequest buildGetAuctionsRequest() {
        return GetAuctionsRequest.newBuilder().build();
    }

    private Auction getAuctionById(List<Auction> list, String id) {
        return list.stream()
                .filter(auction -> auction.getId().equals(id))
                .findAny()
                .orElse(null);
    }

    private boolean isInvalidBid(Double bid, Double currentBid) {
        return bid <= currentBid;
    }

    private SendBidRequest buildSendBidRequest(String id, Double bid, String username) {
        return SendBidRequest.newBuilder()
                .setId(id)
                .setBid(bid)
                .setUsername(username)
                .build();
    }

    Void createAuctionAndSendMessage() {
        retryable();

        CreateAuctionRequest createAuctionRequest = buildCreateAuctionRequestFromUserInput();
        CreateAuctionResponse response = getCreateAuction(createAuctionRequest);

        sendCreateAuctionMessage(response);

        return null;
    }

    private CreateAuctionResponse getCreateAuction(CreateAuctionRequest createAuctionRequest) {
        CreateAuctionResponse response = null;
        try {
            response = connectionProperties.getStub().createAuction(createAuctionRequest);
        } catch (StatusRuntimeException e) {
            retryable();
            this.getCreateAuction(createAuctionRequest);
        }
        return response;
    }

    private CreateAuctionRequest buildCreateAuctionRequestFromUserInput() {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter the product you want to auction:");
        String product = sc.nextLine();
        System.out.println("Enter the initial value:");
        Double initialValue = sc.nextDouble();

        Auction auction = buildAuction(username, product, initialValue);

        return buildCreateAuctionRequest(auction);
    }

    private void sendCreateAuctionMessage(CreateAuctionResponse createAuctionResponse) {

        if (createAuctionResponse.getSuccess()) {
            System.out.println("Auction was created successfully.");
        } else {
            System.out.println("An error ocurred creating the auction.");
        }
        System.out.println(SEPARATOR2);
    }

    private CreateAuctionRequest buildCreateAuctionRequest(Auction auction) {
        return CreateAuctionRequest.newBuilder()
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


    private Integer getRandomPort(List<Integer> portList) {
        return portList.get(new Random().nextInt(portList.size()));
    }

    private Integer getRandomNode(Integer num) {
        return new Random().nextInt(num);
    }


    private List<List<Integer>> getServerPorts() {
        List<List<Integer>> serverPorts = new ArrayList<>();

        for (int currentNode = 0; currentNode < NUMBER_OF_NODES; currentNode++) {
            serverPorts.add(getServerPortsFromNode(currentNode));
        }

        return serverPorts;
    }

    private List<Integer> getServerPortsFromNode(int currentNode) {
        List<Integer> portList = new ArrayList<>();
        for (int currentServer = 0; currentServer < NUMBER_OF_REPLICAS; currentServer++) {
            portList.add(getServerPort(currentServer, currentNode));
        }
        return portList;
    }

    private Integer getServerPort(int serverPosition, int currentNode) {
        return BASE_PORT + getNodeOffset(currentNode) + serverPosition;
    }

    private Integer getNodeOffset(int currentNode) {
        return currentNode * NODE_PORT_DIFFERENCE;
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private void retryable() {
        Integer port = getRandomPort(connectionProperties.getNodePortList());
        System.out.println("Connecting on port: " + port);
        ManagedChannel channel = buildChannel(getServerHost(), port);
        AuctionServiceGrpc.AuctionServiceBlockingStub stub = AuctionServiceGrpc.newBlockingStub(channel);
        connectionProperties.setChannel(channel);
        connectionProperties.setStub(stub);
    }

}
