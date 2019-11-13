package domain;

public class SendBidLog {

    private Integer id;
    private Double bid;
    private Boolean isServer;
    private Integer port;
    private String username;

    public SendBidLog() {
    }

    public SendBidLog(Integer id, Double bid, Boolean isServer, Integer port, String username) {
        this.id = id;
        this.bid = bid;
        this.isServer = isServer;
        this.port = port;
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getBid() {
        return bid;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public Boolean getServer() {
        return isServer;
    }

    public void setServer(Boolean server) {
        isServer = server;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
