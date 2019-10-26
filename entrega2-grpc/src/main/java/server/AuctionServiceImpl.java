package server;

import java.util.ArrayList;
import java.util.List;

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
    public void getAuctionsService(GetAuctionsRequest getAuctionsRequest,
            StreamObserver<GetAuctionsResponse> responseObserver) {

        AuctionMapper mapper = new AuctionMapper();

        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        List<AuctionData> auctionsData = new ArrayList<AuctionData>(jsonLoader.loadList("auctions.json",
                AuctionData.class));

        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        System.out.println(auctions);
        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    public static void main(String[] args) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

        auctionService.getAuctionsService(GetAuctionsRequest.newBuilder().build(),
                new StreamObserver<GetAuctionsResponse>() {
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
