package server;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.AuctionServiceGrpc.AuctionServiceImplBase;
import util.JsonLoader;

public class AuctionServiceImpl extends AuctionServiceImplBase {

    private static Integer SERVER_PORT = getServerPort();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();

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
        //List<AuctionServiceBlockingStub> stubs = buildStubsMap(getAuctionsRequest.getPort());

        List<AuctionData> auctionsData = loadAuctions(getAuctionsRequest.getPort());
        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        if(!getAuctionsRequest.getIsServer() ) {

            BidiMap<Integer, AuctionServiceBlockingStub> serversStubs = buildStubsMap(getAuctionsRequest.getPort());
            serversStubs.forEach((port, stub) -> {
                GetAuctionsResponse response = stub.getAuctions(buildGetAuctionsRequest(port, TRUE));
                auctions.addAll(response.getAuctionsList());
            });
        }


        System.out.println(auctions);
        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    private GetAuctionsRequest buildGetAuctionsRequest(Integer port, Boolean isServer) {
        return GetAuctionsRequest.newBuilder().setPort(port).setIsServer(isServer).build();
    }

    @Override
    public void sendBid(SendBidRequest sendBidRequest, StreamObserver<SendBidResponse> responseObserver) {

        AuctionMapper mapper = new AuctionMapper();

        List<AuctionData> auctionsData = loadAuctions(getServerPort());
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

    private List<AuctionData> loadAuctions(Integer port) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        ArrayList<AuctionData> auctionsList = new ArrayList<>();
        List<AuctionServiceBlockingStub> stubs = new ArrayList<>();

        return new ArrayList<AuctionData>(jsonLoader.loadList("auctions" + (port - SERVER_PORT) +".json",
                AuctionData.class));
    }

    private void saveAuctions(List<AuctionData> auctionsToSave) throws IOException {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        jsonLoader.saveFile("auctions0.json", auctionsToSave);
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

    private BidiMap<Integer, AuctionServiceBlockingStub> buildStubsMap(Integer port) {

        BidiMap<Integer, AuctionServiceBlockingStub> stubsMap = new DualHashBidiMap<>();
        List<AuctionServiceBlockingStub> stubs = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            if(SERVER_PORT + i != port) {
                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
                stubsMap.put(SERVER_PORT + i, stub);
            }
        }

        return stubsMap;
    }

    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public static void main(String[] args) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

        auctionService.getAuctions(GetAuctionsRequest.newBuilder().setPort(50001).setIsServer(false).build()

                        , new StreamObserver<GetAuctionsResponse>() {
                    @Override
                    public void onNext(GetAuctionsResponse getAuctionsResponse) {

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
