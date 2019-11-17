package server;

import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

import java.util.Map;

public class ServerConfigs extends ServerInfo {

    private AuctionServiceBlockingStub stub;

    public ServerConfigs() {
    }

    public ServerConfigs(AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }

    public ServerConfigs(Integer port, AuctionServiceBlockingStub stub, Map<String, String> hashTable) {
        super(port, hashTable);
        this.stub = stub;
    }

    public AuctionServiceBlockingStub getStub() {
        return stub;
    }

    public void setStub(AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }
}
