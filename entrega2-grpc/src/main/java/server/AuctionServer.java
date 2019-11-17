package server;

import domain.CreateAuctionLog;
import domain.Log;
import domain.NextId;
import domain.SendBidLog;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import mapper.AuctionData;
import mapper.AuctionMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import util.JsonLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.*;

public class AuctionServer {

    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SERVER_PORT = getServerPort();

    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";

    private BidiMap<Server, ServerConfigs> buildServers() {

        BidiMap<Server, ServerConfigs> serversMap = new DualHashBidiMap<>();

        Map<String, String> hashTable = generateHashTable();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            Server server = buildAndStartAuctionServer(SERVER_PORT + i);
            AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
            serversMap.put(server, new ServerConfigs(SERVER_PORT + i, stub, hashTable));
        }

        return serversMap;
    }

    private Map<String, String> generateHashTable() {
        List<String> list = generateIdList();

        Map<String, String> hashTable = new HashMap<>();
        list.forEach(obj -> hashTable.put(String.valueOf(hashTable.size()), obj));

        return hashTable;
    }

    private List<String> generateIdList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < AuctionServer.NUMBER_OF_SERVERS; i++) {
            list.add(generateSha1Hash(String.valueOf(i)));
        }
        list.sort(Comparator.comparing(String::toLowerCase));
        return list;
    }

    private String generateSha1Hash(String dataToDiggest) {
        return DigestUtils.sha1Hex(dataToDiggest.getBytes(StandardCharsets.UTF_8));
    }

    private ManagedChannel buildChannel(String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    private Server buildAndStartAuctionServer(Integer port) {

        Server server = ServerBuilder
                .forPort(port)
                .addService(new AuctionServiceImpl()).build();

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error starting server.");
            return null;
        }

        return server;
    }

    public void processLogs(BidiMap<Server, ServerConfigs> serversMap) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

        serversMap.forEach((server, serverConfig) -> {
            JsonLoader logAndSnapshotLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN,
                    serverConfig.getPort() - SERVER_PORT));

            NextId nextLogId = auctionService.loadLogOrSnapshotNextId(NEXT_LOG_FILE, logAndSnapshotLoader);
            List<Log> logs = auctionService.loadLogs(nextLogId.getId(), logAndSnapshotLoader);

            NextId nextSnapshotId = auctionService.loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, logAndSnapshotLoader);
            List<AuctionData> snapshot = auctionService.loadSnapshot(logAndSnapshotLoader, nextSnapshotId.getId());

            auctionService.saveAuctions(snapshot, serverConfig.getPort());
            executeLogs(serverConfig, logs, snapshot);
        });
    }

    private void executeLogs(ServerConfigs serverConfigs, List<Log> logs, List<AuctionData> snapshot) {

        logs.forEach(log -> {
            switch (log.getFunction()) {
                case CREATE_AUCTION:
                    serverConfigs.getStub().createAuction(buildCreateAuctionRequestFromLog(log.getLogData().getCreateAuctionData()));
                    break;
                case SEND_BID:
                    serverConfigs.getStub().sendBid(buildSendBidRequestFromLog(log.getLogData().getSendBidData()));
                    break;
            }
        });
    }

    private CreateAuctionRequest buildCreateAuctionRequestFromLog(CreateAuctionLog log) {

        AuctionMapper auctionMapper = new AuctionMapper();

        return CreateAuctionRequest.newBuilder()
                .setId(log.getAuction().getId())
                .setPort(log.getPort())
                .setIsServer(log.getServer())
                .setAuction(auctionMapper.auctionFromAuctionData(log.getAuction()))
                .setIsProcessLogs(TRUE)
                .build();
    }

    private SendBidRequest buildSendBidRequestFromLog(SendBidLog log) {

        return SendBidRequest.newBuilder()
                .setPort(log.getPort())
                .setIsServer(log.getServer())
                .setUsername(log.getUsername())
                .setBid(log.getBid())
                .setId(log.getId())
                .setIsProcessLogs(TRUE)
                .build();
    }

    public static void main(String[] args) {

        AuctionServer auctionServer = new AuctionServer();
        BidiMap<Server, ServerConfigs> serversMap = auctionServer.buildServers();

        System.out.println(serversMap);

        auctionServer.processLogs(serversMap);

        serversMap.forEach((server, serverConfig) -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }


}
