package server;

import java.util.List;

import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

public class ServerConfigs extends ServerInfo {

    private AuctionServiceBlockingStub stub;

    public ServerConfigs() {
    }

    public ServerConfigs(AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }

    public ServerConfigs(Integer port, AuctionServiceBlockingStub stub) {
        super(port);
        this.stub = stub;
    }

    public AuctionServiceBlockingStub getStub() {
        return stub;
    }

    public void setStub(AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }
}
