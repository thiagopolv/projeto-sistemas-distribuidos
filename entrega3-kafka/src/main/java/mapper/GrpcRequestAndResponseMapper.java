package mapper;

import server.SaveAuctionRequest;
import server.SaveBidRequest;

public class GrpcRequestAndResponseMapper {

    public SaveAuctionRequestData saveAuctionRequestDataFromSaveAuctionRequest(SaveAuctionRequest request) {
        AuctionMapper auctionMapper = new AuctionMapper();
        SaveAuctionRequestData saveAuctionRequestData = new SaveAuctionRequestData();

        saveAuctionRequestData.setAuction(auctionMapper.auctionDataFromAuction(request.getAuction()));
        saveAuctionRequestData.setAuctionId(request.getAuctionId());
        saveAuctionRequestData.setServerSufix(request.getServerSufix());

        return saveAuctionRequestData;
    }

    public SaveAuctionRequest saveAuctionRequestFromSaveAuctionRequestData(SaveAuctionRequestData requestData) {
        AuctionMapper auctionMapper = new AuctionMapper();

        return SaveAuctionRequest.newBuilder()
                .setAuction(auctionMapper.auctionFromAuctionData(requestData.getAuction()))
                .setAuctionId(requestData.getAuctionId())
                .setServerSufix(requestData.getServerSufix())
                .build();
    }

    public SaveBidRequest saveBidRequestFromSaveBidRequestData(SaveBidRequestData saveBidRequestData) {
        return SaveBidRequest.newBuilder()
                .setAuctionId(saveBidRequestData.getAuctionId())
                .setBid(saveBidRequestData.getBid())
                .setUsername(saveBidRequestData.getUsername())
                .setHashTableId(saveBidRequestData.getHashTableId())
                .setServerSufix(saveBidRequestData.getServerSufix())
                .build();
    }

    public SaveBidRequestData saveAuctionRequestDataFromSaveAuctionRequest(SaveBidRequest request) {
        SaveBidRequestData saveBidRequestData = new SaveBidRequestData();

        saveBidRequestData.setBid(request.getBid());
        saveBidRequestData.setHashTableId(request.getHashTableId());
        saveBidRequestData.setUsername(request.getUsername());
        saveBidRequestData.setAuctionId(request.getAuctionId());
        saveBidRequestData.setServerSufix(request.getServerSufix());

        return saveBidRequestData;
    }
}
