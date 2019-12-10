package domain;

import mapper.AuctionData;

public class SaveAuctionLog {

    private AuctionData auction;

    public SaveAuctionLog() {
    }

    public AuctionData getAuction() {
        return auction;
    }

    public void setAuction(AuctionData auction) {
        this.auction = auction;
    }

}
