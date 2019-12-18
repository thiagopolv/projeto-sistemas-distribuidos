package domain;

import java.time.LocalDateTime;
import java.util.List;

import mapper.AuctionData;

public class AuctionDatabaseData {

    private List<AuctionData> auctions;
    private String lastUpdate;

    public AuctionDatabaseData() {
    }

    public AuctionDatabaseData(List<AuctionData> auctions, String lastUpdate) {
        this.auctions = auctions;
        this.lastUpdate = lastUpdate;
    }

    public List<AuctionData> getAuctions() {
        return auctions;
    }

    public void setAuctions(List<AuctionData> auctions) {
        this.auctions = auctions;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
