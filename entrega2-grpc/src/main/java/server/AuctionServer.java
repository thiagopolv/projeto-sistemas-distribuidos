package server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import util.ConfigProperties;

import java.io.IOException;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class AuctionServer {

    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SERVER_PORT = getServerPort();

    private BidiMap<Server, ServerInfo> serversInfo;

    public BidiMap<Server, ServerInfo> getServersInfo() {
        return serversInfo;
    }

    public void setServersInfo(BidiMap<Server, ServerInfo> serversInfo) {
        this.serversInfo = serversInfo;
    }

    private BidiMap<Server, ServerInfo> buildServers() {

        BidiMap<Server, ServerInfo> serversMap = new DualHashBidiMap<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {

            Server server = buildAndStartAuctionserver(SERVER_PORT + i);
            AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(getServerHost(), SERVER_PORT + i));
            serversMap.put(server, new ServerInfo(stub));
        }

        return serversMap;
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    private Server buildAndStartAuctionserver(Integer port) {

        Server server = ServerBuilder
                .forPort(port)
                .addService(new AuctionServiceImpl()).build();

        try {
            server.start();
            server.awaitTermination();
        } catch (IOException e) {
            System.out.println("Error starting server.");
            return null;
        } catch (InterruptedException e) {
            System.out.println("Error terminating server.");
            return null;
        }

        return server;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        ConfigProperties configProperties = ConfigProperties.getProperties();

        Server server = ServerBuilder
                .forPort(getServerPort())
                .addService(new AuctionServiceImpl()).build();

        server.start();
        server.awaitTermination();
    }
}
