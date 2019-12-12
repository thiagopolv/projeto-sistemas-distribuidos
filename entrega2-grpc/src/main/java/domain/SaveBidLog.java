package domain;

public class SaveBidLog {

    private String auctionId;
    private Double bid;
    private String username;
    private Integer hashTableId;

    public SaveBidLog() {
    }

    public SaveBidLog(String auctionId, Double bid, String username, Integer hashTableId) {
        this.auctionId = auctionId;
        this.bid = bid;
        this.username = username;
        this.hashTableId = hashTableId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public Double getBid() {
        return bid;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getHashTableId() {
        return hashTableId;
    }

    public void setHashTableId(Integer hashTableId) {
        this.hashTableId = hashTableId;
    }
}
