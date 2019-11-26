package domain;

import mapper.AuctionData;

public class CreateAuctionLog {

    private AuctionData auction;

    public CreateAuctionLog() {
    }

    public AuctionData getAuction() {
        return auction;
    }

    public void setAuction(AuctionData auction) {
        this.auction = auction;
    }

}
