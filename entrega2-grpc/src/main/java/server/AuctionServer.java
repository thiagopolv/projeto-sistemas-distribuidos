package server;

import domain.CreateAuctionLog;
import domain.Log;
import domain.NextId;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import mapper.AuctionData;
import mapper.AuctionMapper;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import util.JsonLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.String.format;
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

    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String LOGS_FILE_NAME_PATTERN = "logs%d.json";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";
    private static final String SNAPSHOT_FILE_NAME_FORMAT = "snapshot%d.json";

    private BidiMap<Server, ServerInfo> serversInfo;

    public BidiMap<Server, ServerInfo> getServersInfo() {
        return serversInfo;
    }

    public void setServersInfo(BidiMap<Server, ServerInfo> serversInfo) {
        this.serversInfo = serversInfo;
    }

    public List<AuctionServiceBlockingStub> buildStubs() {

        List<AuctionServiceBlockingStub> stubs = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
            stubs.add(stub);
        }

        return stubs;
    }

    public BidiMap<Server, ServerConfigs> buildServers() throws IOException, InterruptedException {

        BidiMap<Server, ServerConfigs> serversMap = new DualHashBidiMap<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {

            Server server = buildAndStartAuctionServer(SERVER_PORT + i);
            AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
            serversMap.put(server, new ServerConfigs(SERVER_PORT + i, stub));
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

    private Server buildAndStartAuctionServer(Integer port) throws IOException, InterruptedException {

        Server server = ServerBuilder
                .forPort(port)
                .addService(new AuctionServiceImpl()).build();

        try {
            server.start();
//            server.awaitTermination();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error starting server.");
            return null;
//        } catch (InterruptedException e) {
//            System.out.println("Error terminating server.");
//            return null;
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
                    serverConfigs.getStub().createAuction(buildCreateAuctionRequestFromLog(log));
                    break;
            }
        });
    }

    private CreateAuctionRequest buildCreateAuctionRequestFromLog(Log log) {

        AuctionMapper auctionMapper = new AuctionMapper();

        return CreateAuctionRequest.newBuilder()
                .setPort(log.getLogData().getCreateAuctionData().getPort())
                .setIsServer(log.getLogData().getCreateAuctionData().getServer())
                .setAuction(auctionMapper.auctionFromAuctionData(log.getLogData().getCreateAuctionData().getAuction()))
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

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


//        List<AuctionServiceBlockingStub> stubs = auctionServer.buildStubs();



    }


}
