package client;

import server.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static util.ConfigProperties.getDaysToExpireAuction;

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

    ClientService(ClientConnectionProperties connectionProperties, String username) {
        this.connectionProperties = connectionProperties;
        this.username = username;
    }

    void resolveClientAction(ClientAction clientAction) {
        clientAction.clientActionFunction.apply(this);
    }

    Void printAuctions() {
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
        return connectionProperties.getStub().getAuctions(buildGetAuctionsRequest());
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
        CreateAuctionRequest createAuctionRequest = buildCreateAuctionRequestFromUserInput();
        CreateAuctionResponse response = connectionProperties.getStub().createAuction(createAuctionRequest);
        sendCreateAuctionMessage(response);
        return null;
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
}
