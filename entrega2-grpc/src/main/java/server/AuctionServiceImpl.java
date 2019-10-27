package server;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.EMPTY_LIST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import util.JsonLoader;

public class AuctionServiceImpl extends AuctionServiceGrpc.AuctionServiceImplBase {

    @Override
    public void auction(AuctionRequest auctionRequest, StreamObserver<AuctionResponse> responseObserver) {

        AuctionResponse response = AuctionResponse.newBuilder()
                .setAction(auctionRequest.getAction())
                .setData(auctionRequest.getData())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAuctions(GetAuctionsRequest getAuctionsRequest,
            StreamObserver<GetAuctionsResponse> responseObserver) {

        AuctionMapper mapper = new AuctionMapper();

        List<AuctionData> auctionsData = loadAuctions();

        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        System.out.println(auctions);
        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void sendBid(SendBidRequest sendBidRequest, StreamObserver<SendBidResponse> responseObserver) {

        AuctionMapper mapper = new AuctionMapper();

        List<AuctionData> auctionsData = loadAuctions();
        AuctionData auctionToChange = getAuctionById(auctionsData, sendBidRequest.getId());
        auctionToChange.setCurrentBid(verifyBid(auctionToChange.getCurrentBid(), sendBidRequest.getBid()));

        List<Auction> auctionsResponse = mapper.auctionListFromAuctionDataList(auctionsData);

        Boolean sucessfulNewBid = successfulNewBid(auctionToChange.getCurrentBid(), sendBidRequest.getBid());

        SendBidResponse sendBidResponse = buildResponseAndPersistIfNecessary(sucessfulNewBid, auctionsData,
                auctionsResponse);

        responseObserver.onNext(sendBidResponse);
        responseObserver.onCompleted();
    }

    private SendBidResponse buildResponseAndPersistIfNecessary(Boolean success, List<AuctionData> auctionsToSave,
            List<Auction> auctions) {

        if (success) {
            try {
                saveAuctions(auctionsToSave);
            } catch (IOException e) {
                System.out.println("Error saving data.");
                return buildFailSendNewBidResponse(auctions);
            }
            return buildSuccessfulSendNewBid(auctions);
        }

        return buildFailSendNewBidResponse(auctions);
    }

    private SendBidResponse buildSuccessfulSendNewBid(List<Auction> auctions) {
        return SendBidResponse.newBuilder()
                .setSuccess(TRUE)
                .addAllAuctions(auctions)
                .build();
    }

    private SendBidResponse buildFailSendNewBidResponse(List<Auction> auctions) {
        return SendBidResponse.newBuilder()
                .setSuccess(FALSE)
                .addAllAuctions(auctions)
                .build();
    }

    private List<AuctionData> loadAuctions() {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        return new ArrayList<AuctionData>(jsonLoader.loadList("auctions.json",
                AuctionData.class));
    }

    private void saveAuctions(List<AuctionData> auctionsToSave) throws IOException {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        jsonLoader.saveFile("auctions.json", auctionsToSave);
    }

    private AuctionData getAuctionById(List<AuctionData> list, Integer id) {

        return list.stream()
                .filter(auction -> auction.getId().equals(id))
                .collect(Collectors.toList())
                .get(0);
    }

    private Double verifyBid(Double currentBid, Double newBid) {
        return newBid > currentBid ? newBid : currentBid;
    }

    private Boolean successfulNewBid(Double currentBid, Double newBid) {
        return currentBid.equals(newBid);
    }

    public static void main(String[] args) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

        auctionService.sendBid(SendBidRequest.newBuilder()
                        .setId(1)
                        .setBid(10.0)
                        .build(), new StreamObserver<SendBidResponse>() {
            @Override
            public void onNext(SendBidResponse sendBidResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
}
