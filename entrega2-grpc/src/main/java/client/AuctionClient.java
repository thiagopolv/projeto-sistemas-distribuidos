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
import util.ConfigProperties;

import static client.ClientAction.INVALID;
import static client.ClientAction.getEnumMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AuctionClient {

    private static final String ROOT_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private static final String CONFIG_PATH = ROOT_PATH + "application.properties";
    private static final String SEND_BID_SUCCESS_MESSAGE = "\nYour bid was sent successfully.\n\nUpdated auctions " +
            "list:";
    private static final String SEND_BID_FAIL_MESSAGE = "There was an error sending your bid.\n\nUpdated auctions " +
            "list:";
    private static final String DISCONNECT = "DIS";

    private GetAuctionsRequest buildGetAuctionsRequest() {
        return GetAuctionsRequest.newBuilder().build();
    }

    public  GetAuctionsResponse getAuctions(AuctionServiceBlockingStub stub) {
        return stub.getAuctions(buildGetAuctionsRequest());
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public void chooseAction(AuctionServiceBlockingStub stub, List auctions) {
        String action = EMPTY;
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("Choose an action:");
        System.out.println("1 - LIST auctions; / 2 - SEND bid; / ");
        System.out.println("3 - REFresh auctions; / 4 - DISconnect.");
        System.out.println("==================================================");

        do {
            System.out.println("\nAction: ");
            action = scanner.nextLine();

            executeAction(action.toUpperCase(), stub, auctions);

        } while (!action.toUpperCase().equals(DISCONNECT));
    }


    private void executeAction(String action, AuctionServiceBlockingStub stub, List auctions) {
        ClientAction clientAction = getEnumMap().containsValue(action) ? getEnumMap().getKey(action) : INVALID;

        switch(clientAction) {
            case LIST :
                auctions = getAuctions(stub).getAuctionsList();
                System.out.println(auctions + "\n\n");
                break;
            case SEND_BID:
                auctions = getAuctions(stub).getAuctionsList();
                System.out.println("\nUpdated auctions list:\n" + sendBidAndReturnUpdatedAuctionList(stub,
                        auctions));
                break;
            case REFRESH:
                break;
            case DISCONNECT:
                System.out.println("Disconnected.");
                break;
            case INVALID:
                System.out.println("Invalid action, please send another.");
        }
    }

    private List<Auction> sendBidAndReturnUpdatedAuctionList(AuctionServiceBlockingStub stub, List<Auction> auctions) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Insert the auction Id you want to send the bid: ");
        Integer id = sc.nextInt();
        System.out.println("Insert bid value: ");
        Double bid = sc.nextDouble();

        Double currentBid = getAuctionById(auctions, id).getCurrentBid();

        if (isInvalidBid(bid, currentBid)) {
            System.out.println("Your bid is less than " + currentBid);
            return getAuctions(stub).getAuctionsList();
        }

        SendBidResponse sendBidResponse = stub.sendBid(buildSendBidRequest(id, bid));

        String message = sendBidResponse.getSuccess() ? SEND_BID_SUCCESS_MESSAGE : SEND_BID_FAIL_MESSAGE;
        System.out.println(message + "\n");
        return sendBidResponse.getAuctionsList();
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

    private SendBidRequest buildSendBidRequest(Integer id, Double bid) {
        return SendBidRequest.newBuilder()
                .setId(id)
                .setBid(bid)
                .build();
    }

    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        ConfigProperties configProperties = ConfigProperties.getProperties();
        ManagedChannel channel = client.buildChannel(getServerHost(), getServerPort());
        AuctionServiceBlockingStub stub = AuctionServiceGrpc.newBlockingStub(channel);
        List<Auction> auctions = client.getAuctions(stub).getAuctionsList();

        client.chooseAction(stub, auctions);
        channel.shutdown();
    }
}
