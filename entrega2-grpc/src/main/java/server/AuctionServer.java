package server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class AuctionServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        Server server = ServerBuilder
                .forPort(8080)
                .addService(new AuctionServiceImpl()).build();

        server.start();
        server.awaitTermination();
    }
}
