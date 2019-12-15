//package server;
//
//import static io.grpc.ManagedChannelBuilder.forAddress;
//import static java.lang.Boolean.TRUE;
//import static java.lang.String.format;
//import static server.AuctionServiceGrpc.newBlockingStub;
//import static util.ConfigProperties.getKafkaHost;
//import static util.ConfigProperties.getNumberOfNodes;
//import static util.ConfigProperties.getNumberOfServers;
//import static util.ConfigProperties.getServerHost;
//import static util.ConfigProperties.getServerPort;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.collections4.BidiMap;
//import org.apache.commons.collections4.bidimap.DualHashBidiMap;
//
//import consumer.AuctionConsumer;
//import domain.SaveBidLog;
//import io.grpc.ManagedChannel;
//import io.grpc.Server;
//import io.grpc.ServerBuilder;
//import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
//import util.ConfigProperties;
//
//public class AuctionServer {
//
//    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
//    private static final String SERVER_HOST = getServerHost();
//    private static final Integer SERVER_PORT = getServerPort();
//    private static final String KAFKA_HOST = getKafkaHost();
//    private static final Integer NUMBER_OF_NODES = getNumberOfNodes();
//
//    private static final String TOPIC_NAME = "auction-topic-%d";
//    private static final String GROUP_ID_NAME = "auction-group-%d";
//
//    private Map<String, String> hashTable;
//
//    public AuctionServer() {
//        this.hashTable = generateHashTable();
//    }
//
//    private BidiMap<Server, ServerConfigs> buildServers() {
//
//        BidiMap<Server, ServerConfigs> serversMap = new DualHashBidiMap<>();
//
//        Map<String, String> hashTable = generateHashTable();
//
//        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
//            AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
//            ServerConfigs serverConfigs = new ServerConfigs(SERVER_PORT + i, stub, hashTable);
//            Server server = buildAndStartAuctionServer(SERVER_PORT + i, serverConfigs);
//            serversMap.put(server, serverConfigs);
//        }
//
//        return serversMap;
//    }
//
//    private Map<String, String> generateHashTable() {
//        List<String> list = generateIdList();
//
//        Map<String, String> hashTable = new HashMap<>();
//        list.forEach(obj -> hashTable.put(String.valueOf(hashTable.size()), obj));
//
//        return hashTable;
//    }
//
//    private List<String> generateIdList() {
//        List<String> list = new ArrayList<>();
//        for (int i = 0; i < AuctionServer.NUMBER_OF_NODES; i++) {
//            list.add(generateSha1Hash(String.valueOf(i)));
//        }
//        list.sort(Comparator.comparing(String::toLowerCase));
//        return list;
//    }
//
//    private String generateSha1Hash(String dataToDigest) {
//        return DigestUtils.sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
//    }
//
//    private ManagedChannel buildChannel(String host, Integer port) {
//        return forAddress(host, port)
//                .usePlaintext()
//                .build();
//    }
//
//    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
//        return newBlockingStub(channel);
//    }
//
//    private Server buildAndStartAuctionServer(Integer port, ServerConfigs serverConfigs) {
//
//        Server server = ServerBuilder
//                .forPort(port)
//                .addService(new AuctionServiceImpl()).build();
//
//        try {
//            server.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.out.println("Error starting server.");
//            return null;
//        }
//
//        return server;
//    }
////
////    private void processLogs(BidiMap<Server, ServerConfigs> serversMap) {
////        AuctionServiceImpl auctionService = new AuctionServiceImpl();
////
////        serversMap.forEach((server, serverConfig) -> {
////            JsonLoader logAndSnapshotLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN,
////                    serverConfig.getPort() - SERVER_PORT));
////
////            NextId nextLogId = auctionService.loadLogOrSnapshotNextId(NEXT_LOG_FILE, logAndSnapshotLoader);
////            List<Log> logs = auctionService.loadLogs(nextLogId.getId(), logAndSnapshotLoader);
////
////            NextId nextSnapshotId = auctionService.loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, logAndSnapshotLoader);
////            List<AuctionData> snapshot = auctionService.loadSnapshot(logAndSnapshotLoader, nextSnapshotId.getId());
////
////            auctionService.saveAuctions(snapshot, serverConfig.getPort() - SERVER_PORT);
////            executeLogs(serverConfig, logs, snapshot);
////        });
////    }
//
////    private void executeLogs(ServerConfigs serverConfigs, List<Log> logs, List<AuctionData> snapshot) {
////
////        logs.forEach(log -> {
////            switch (log.getFunction()) {
////                case SAVE_AUCTION:
////                    serverConfigs.getStub().saveAuction(
////                            buildCreateAuctionRequestFromLog(log.getLogData().getSaveAuctionData()));
////                    break;
////                case SAVE_BID:
////                    serverConfigs.getStub().saveBid(buildSendBidRequestFromLog(log.getLogData().getSaveBidData()));
////                    break;
////            }
////        });
////    }
//
////    private SaveAuctionRequest buildCreateAuctionRequestFromLog(SaveAuctionLog log) {
////
////        AuctionMapper auctionMapper = new AuctionMapper();
////
////        return SaveAuctionRequest.newBuilder()
////                .setAuction(auctionMapper.auctionFromAuctionData(log.getAuction()))
////                .setProcessingLogs(TRUE)
////                .setHashTableId(log.getHashTableId())
////                .build();
////    }
//
//    private SaveBidRequest buildSendBidRequestFromLog(SaveBidLog log) {
//
//        return SaveBidRequest.newBuilder()
//                .setUsername(log.getUsername())
//                .setBid(log.getBid())
//                .setAuctionId(log.getAuctionId())
//                .setProcessingLogs(TRUE)
//                .setHashTableId(log.getHashTableId())
//                .build();
//    }
//
//    private List<AuctionConsumer> buildAuctionConsumers() {
//
//        List<AuctionConsumer> consumers = new ArrayList<>();
//
//        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
//            AuctionConsumer consumer = new AuctionConsumer(
//                    KAFKA_HOST,
//                    format(GROUP_ID_NAME, i),
//                    format(TOPIC_NAME, i % NUMBER_OF_NODES),
//                    i);
//
//            consumers.add(consumer);
//        }
//
////        consumers.forEach(consumer -> new Thread(consumer::run));
//        consumers.forEach(AuctionConsumer::run);
//        return consumers;
//    }
//
//
//    public static void main(String[] args) {
//
//        AuctionServer auctionServer = new AuctionServer();
//
//        BidiMap<Server, ServerConfigs> serversMap = auctionServer.buildServers();
//
//        List<AuctionConsumer> consumers = auctionServer.buildAuctionConsumers();
//
//
//        System.out.println(serversMap);
//
////        auctionServer.processLogs(serversMap);
//
//        serversMap.forEach((server, serverConfig) -> {
//            try {
//                server.awaitTermination();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//
//}