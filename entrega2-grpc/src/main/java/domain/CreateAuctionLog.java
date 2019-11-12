package domain;

import mapper.AuctionData;

public class CreateAuctionLog extends Log {

    private Integer port;
    private AuctionData auction;
    private Boolean isServer;

    public CreateAuctionLog() {
    }

    public CreateAuctionLog(LogFunctions function, Object data, Integer port, AuctionData auction,
            Boolean isServer) {
        super(function, data);
        this.port = port;
        this.auction = auction;
        this.isServer = isServer;
    }

    public CreateAuctionLog(Integer port, AuctionData auction, Boolean isServer) {
        this.port = port;
        this.auction = auction;
        this.isServer = isServer;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public AuctionData getAuction() {
        return auction;
    }

    public void setAuction(AuctionData auction) {
        this.auction = auction;
    }

    public Boolean getServer() {
        return isServer;
    }

    public void setServer(Boolean server) {
        isServer = server;
    }

    public static void main(String[] args) {
        Log log = new Log();

        log.setFunction(LogFunctions.CREATE_AUCTION);
        log.setData(new CreateAuctionLog(1, new AuctionData(), true));

        CreateAuctionLog loga = (CreateAuctionLog) log.getData();

        System.out.println(loga);
    }
}
