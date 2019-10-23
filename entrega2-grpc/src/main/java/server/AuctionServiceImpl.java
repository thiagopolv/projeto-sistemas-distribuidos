package server;

import io.grpc.stub.StreamObserver;

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
}
