package server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import util.ConfigProperties;

import java.io.IOException;

import static util.ConfigProperties.getServerPort;

public class AuctionServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        ConfigProperties configProperties = ConfigProperties.getProperties();

        Server server = ServerBuilder
                .forPort(getServerPort())
                .addService(new AuctionServiceImpl()).build();

        server.start();
        server.awaitTermination();
    }
}
