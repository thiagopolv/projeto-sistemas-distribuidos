package domain;

public class SaveBidLog {

    private String id;
    private Double bid;
    private String username;
    private Integer hashTableId;

    public SaveBidLog() {
    }

    public SaveBidLog(String id, Double bid, String username, Integer hashTableId) {
        this.id = id;
        this.bid = bid;
        this.username = username;
        this.hashTableId = hashTableId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
