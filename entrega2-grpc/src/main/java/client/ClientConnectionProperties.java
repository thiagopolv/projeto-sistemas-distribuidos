package client;

import io.grpc.ManagedChannel;
import server.AuctionServiceGrpc;

public class ClientConnectionProperties {
    private AuctionServiceGrpc.AuctionServiceBlockingStub stub;
    private Integer port;
    private ManagedChannel channel;

    ClientConnectionProperties(AuctionServiceGrpc.AuctionServiceBlockingStub stub, Integer port, ManagedChannel channel) {
        this.stub = stub;
        this.port = port;
        this.channel = channel;
    }

    public AuctionServiceGrpc.AuctionServiceBlockingStub getStub() {
        return stub;
    }

    public void setStub(AuctionServiceGrpc.AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void setChannel(ManagedChannel channel) {
        this.channel = channel;
    }
}
