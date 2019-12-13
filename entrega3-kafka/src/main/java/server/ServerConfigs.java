package server;

import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

import java.util.Map;

public class ServerConfigs extends ServerInfo {

    private AuctionServiceBlockingStub stub;

    public ServerConfigs(Integer serverPort, AuctionServiceBlockingStub stub, Map<String, HashLimits> hashTable) {
        super(serverPort, hashTable);
        this.stub = stub;
    }

    public AuctionServiceBlockingStub getStub() {
        return stub;
    }

    public void setStub(AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }
}
