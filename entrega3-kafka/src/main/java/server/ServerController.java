package server;

import domain.Log;
import domain.NextId;
import domain.SaveAuctionLog;
import domain.SaveBidLog;
import io.grpc.Server;
import mapper.AuctionData;
import mapper.AuctionMapper;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import util.JsonLoader;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerPort;

public class ServerController {
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final Integer SERVER_PORT = getServerPort();

    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";

    private BidiMap<Server, ServerConfigs> buildServers() {

        BidiMap<Server, ServerConfigs> serversMap = new DualHashBidiMap<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            serversMap.putAll(new ServerFactory().bootstrap(i));
        }

        return serversMap;
    }

//    private void processLogs(BidiMap<Server, ServerConfigs> serversMap) {
//        AuctionServiceImpl auctionService = new AuctionServiceImpl();
//
//        serversMap.forEach((server, serverConfig) -> {
//            JsonLoader logAndSnapshotLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN,
//                    serverConfig.getPort() - SERVER_PORT));
//
//            NextId nextLogId = auctionService.loadLogOrSnapshotNextId(NEXT_LOG_FILE, logAndSnapshotLoader);
//            List<Log> logs = auctionService.loadLogs(nextLogId.getId(), logAndSnapshotLoader);
//
//            NextId nextSnapshotId = auctionService.loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, logAndSnapshotLoader);
//            List<AuctionData> snapshot = auctionService.loadSnapshot(logAndSnapshotLoader, nextSnapshotId.getId());
//
//            auctionService.saveAuctions(snapshot, serverConfig.getPort() - SERVER_PORT);
//            executeLogs(serverConfig, logs, snapshot);
//        });
//    }

//    private void executeLogs(ServerConfigs serverConfigs, List<Log> logs, List<AuctionData> snapshot) {
//
//        logs.forEach(log -> {
//            switch (log.getFunction()) {
//                case SAVE_AUCTION:
//                    serverConfigs.getStub().saveAuction(buildCreateAuctionRequestFromLog(log.getLogData().getSaveAuctionData()));
//                    break;
//                case SAVE_BID:
//                    serverConfigs.getStub().saveBid(buildSendBidRequestFromLog(log.getLogData().getSaveBidData()));
//                    break;
//            }
//        });
//    }
//
//    private SaveAuctionRequest buildCreateAuctionRequestFromLog(SaveAuctionLog log) {
//
//        AuctionMapper auctionMapper = new AuctionMapper();
//
//        return SaveAuctionRequest.newBuilder()
//                .setAuction(auctionMapper.auctionFromAuctionData(log.getAuction()))
//                .setHashTableId(log.getHashTableId())
//                .build();
//    }

    private SaveBidRequest buildSendBidRequestFromLog(SaveBidLog log) {

        return SaveBidRequest.newBuilder()
                .setUsername(log.getUsername())
                .setBid(log.getBid())
                .setAuctionId(log.getAuctionId())
                .setProcessingLogs(TRUE)
                .setHashTableId(log.getHashTableId())
                .build();
    }

    public static void main(String[] args) {

        ServerController serverController = new ServerController();

        BidiMap<Server, ServerConfigs> serversMap = serverController.buildServers();

        System.out.println(serversMap);

        serversMap.forEach((server, serverConfig) -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}