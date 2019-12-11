package domain;

import mapper.AuctionData;

public class SaveAuctionLog {

    private AuctionData auction;
    private Integer hashTableId;
    private String auctionId;

    public SaveAuctionLog(AuctionData auction, Integer hashTableId, String auctionId) {
        this.auction = auction;
        this.hashTableId = hashTableId;
        this.auctionId = auctionId;
    }

    public SaveAuctionLog(Integer hashTableId, String auctionId) {
        this.hashTableId = hashTableId;
        this.auctionId = auctionId;
    }

    public SaveAuctionLog() {
    }

    public AuctionData getAuction() {
        return auction;
    }

    public void setAuction(AuctionData auction) {
        this.auction = auction;
    }

    public Integer getHashTableId() {
        return hashTableId;
    }

    public void setHashTableId(Integer hashTableId) {
        this.hashTableId = hashTableId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}
