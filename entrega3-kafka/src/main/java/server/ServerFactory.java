package server;

import config.ServerConfig;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import util.ConfigProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static server.AuctionServiceGrpc.newBlockingStub;

@SuppressWarnings("WeakerAccess")
public class ServerFactory {
    private static final String SERVER_HOST = ConfigProperties.getServerHost();
    private ServerConfig serverConfig;
    private Integer SERVER_PORT;

    public ServerFactory() {

    }

    public ServerFactory(ServerConfig serverBaseConfig, Integer serverPort) {
        this.serverConfig = serverBaseConfig;
        this.SERVER_PORT = serverPort;
    }


    public void bootstrap() {
//        AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT));
        Server server = Optional.ofNullable(buildAndStartAuctionServer(serverConfig))
                .orElseThrow(NullPointerException::new);
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ManagedChannel buildChannel(String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    private static String generateSha1Hash(String dataToDigest) {
        return DigestUtils.sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
    }

    private Server buildAndStartAuctionServer(ServerConfig serverConfig) {
        Server server = ServerBuilder
                .forPort(SERVER_PORT)
                .addService(new AuctionServiceImpl(serverConfig)).build();

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error starting server.");
            return null;
        }

        return server;
    }

    public void start() {
        bootstrap();
    }
}
