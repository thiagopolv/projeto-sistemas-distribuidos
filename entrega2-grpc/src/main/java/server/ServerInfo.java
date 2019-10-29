package server;

import java.util.ArrayList;
import java.util.List;

import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

public class ServerInfo {

    private AuctionServiceBlockingStub serverStub;
    private List<Integer> idsInServer;

    public ServerInfo() {
    }

    public ServerInfo(AuctionServiceBlockingStub serverStub) {
        this.serverStub = serverStub;
        this.idsInServer = new ArrayList<>();
    }

    public ServerInfo(AuctionServiceBlockingStub serverStub, List<Integer> idsInServer) {
        this.serverStub = serverStub;
        this.idsInServer = idsInServer;
    }

    public AuctionServiceBlockingStub getServerStub() {
        return serverStub;
    }

    public void setServerStub(AuctionServiceBlockingStub serverStub) {
        this.serverStub = serverStub;
    }

    public List<Integer> getIdsInServer() {
        return idsInServer;
    }

    public void setIdsInServer(List<Integer> idsInServer) {
        this.idsInServer = idsInServer;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverStub=" + serverStub +
                ", idsInServer=" + idsInServer +
                '}';
    }
}
