package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.AuctionRequest;
import server.AuctionResponse;
import server.AuctionServiceGrpc;
import util.ConfigProperties;

import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

public class AuctionClient {

    private static final String ROOT_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private static final String CONFIG_PATH = ROOT_PATH + "application.properties";

    public static void main(String[] args) {

        ConfigProperties configProperties = ConfigProperties.getProperties();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(getServerHost(), getServerPort())
                .usePlaintext()
                .build();

        AuctionServiceGrpc.AuctionServiceBlockingStub stub = AuctionServiceGrpc.newBlockingStub(channel);

        AuctionResponse auctionResponse = stub.auction(AuctionRequest.newBuilder()
            .setAction(ClientActions.STARTED.name())
            .setData(null)
            .build());

        channel.shutdown();
    }
}
