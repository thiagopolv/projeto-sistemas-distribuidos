package mapper;

public class SaveAuctionRequestData {

    private AuctionData auction;
    private String auctionId;
    private Integer serverSufix;

    public SaveAuctionRequestData() {
    }

    public SaveAuctionRequestData(AuctionData auction, String auctionId, Integer serverSufix) {
        this.auction = auction;
        this.auctionId = auctionId;
        this.serverSufix = serverSufix;
    }

    public AuctionData getAuction() {
        return auction;
    }

    public void setAuction(AuctionData auction) {
        this.auction = auction;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public Integer getServerSufix() {
        return serverSufix;
    }

    public void setServerSufix(Integer serverSufix) {
        this.serverSufix = serverSufix;
    }
}
