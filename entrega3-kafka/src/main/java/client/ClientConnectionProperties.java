package client;

import io.grpc.ManagedChannel;
import server.AuctionServiceGrpc;

import java.util.List;

public class ClientConnectionProperties {
    private AuctionServiceGrpc.AuctionServiceBlockingStub stub;
    private List<Integer> nodePortList;
    private ManagedChannel channel;

    ClientConnectionProperties(AuctionServiceGrpc.AuctionServiceBlockingStub stub, List<Integer> nodePortList, ManagedChannel channel) {
        this.stub = stub;
        this.nodePortList = nodePortList;
        this.channel = channel;
    }

    public ClientConnectionProperties(List<Integer> nodePortList) {
        this.nodePortList = nodePortList;
    }

    public AuctionServiceGrpc.AuctionServiceBlockingStub getStub() {
        return stub;
    }

    public void setStub(AuctionServiceGrpc.AuctionServiceBlockingStub stub) {
        this.stub = stub;
    }

    public List<Integer> getNodePortList() {
        return nodePortList;
    }

    public void setNodePortList(List<Integer> nodePortList) {
        this.nodePortList = nodePortList;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void setChannel(ManagedChannel channel) {
        this.channel = channel;
    }
}
