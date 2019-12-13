package server;

import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.*;

@SuppressWarnings("WeakerAccess")
public class ServerFactory {
    private static final int NUMBER_OF_NODES = getNumberOfNodes();
    private static final int LAST_BASE_HASH = getLastBaseHash();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SERVER_PORT = getServerPort();


    public Map<Server, ServerConfigs> bootstrap() {
        Map<String, HashLimits> hashTable = generateHashTable();

        AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT));
        ServerConfigs serverConfigs = new ServerConfigs(SERVER_PORT, stub, hashTable);
        Server server = Optional.ofNullable(buildAndStartAuctionServer(serverConfigs))
                .orElseThrow(NullPointerException::new);

        return ImmutableMap.of(server, serverConfigs);
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

    private Map<String, HashLimits> generateHashTable() {
        Map<String, HashLimits> hashTable = new HashMap<>();

        for (int i = 0; i < NUMBER_OF_NODES; i++) {
            hashTable.put(String.valueOf(i), buildNodeHashLimits(i));
        }

        return hashTable;
    }

    private HashLimits buildNodeHashLimits(int iterator) {
        String init = Integer.toHexString((LAST_BASE_HASH / NUMBER_OF_NODES * iterator));

        if (isLastElement(iterator)) {
            return new HashLimits(init, Integer.toHexString(LAST_BASE_HASH));
        }

        String end = Integer.toHexString(LAST_BASE_HASH / NUMBER_OF_NODES * (iterator + 1) - 1);
        return new HashLimits(init, end);
    }

    private boolean isLastElement(int iterator) {
        return iterator + 1 == NUMBER_OF_NODES;
    }

    private static String generateSha1Hash(String dataToDigest) {
        return DigestUtils.sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
    }

    private Server buildAndStartAuctionServer(ServerConfigs serverConfigs) {
        Server server = ServerBuilder
                .forPort(ServerFactory.SERVER_PORT)
                .addService(new AuctionServiceImpl(serverConfigs)).build();

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error starting server.");
            return null;
        }

        return server;
    }
}
