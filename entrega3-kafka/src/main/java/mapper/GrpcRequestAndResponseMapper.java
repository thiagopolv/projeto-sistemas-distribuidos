package mapper;

import server.SaveAuctionRequest;

public class GrpcRequestAndResponseMapper {

    public SaveAuctionRequestData saveAuctionRequestDataFromSaveAuctionRequest(SaveAuctionRequest request) {
        AuctionMapper auctionMapper = new AuctionMapper();
        SaveAuctionRequestData saveAuctionRequestData = new SaveAuctionRequestData();

        saveAuctionRequestData.setAuction(auctionMapper.auctionDataFromAuction(request.getAuction()));
        saveAuctionRequestData.setAuctionId(request.getAuctionId());
        saveAuctionRequestData.setServerSufix(request.getServerSufix());

        return saveAuctionRequestData;
    }
}
