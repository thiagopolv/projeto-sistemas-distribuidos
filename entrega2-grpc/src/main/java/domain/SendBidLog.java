package domain;

public class SendBidLog {

    private String id;
    private Double bid;
    private String username;

    public SendBidLog() {
    }

    public SendBidLog(String id, Double bid, String username) {
        this.id = id;
        this.bid = bid;
        this.username = username;
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
}
