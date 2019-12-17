package server;

import static domain.LogFunction.SAVE_AUCTION;
import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.getKafkaHost;
import static util.ConfigProperties.getLogSize;
import static util.ConfigProperties.getNumberOfLogs;
import static util.ConfigProperties.getNumberOfNodes;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getSaveCopies;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import config.ServerConfig;
import domain.LogData;
import domain.NextId;
import domain.SaveBidLog;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import mapper.GrpcRequestAndResponseMapper;
import mapper.SaveAuctionRequestData;
import producer.AuctionProducer;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.AuctionServiceGrpc.AuctionServiceImplBase;
import util.JsonLoader;

public class AuctionServiceImpl extends AuctionServiceImplBase {

    private ServerConfig serverConfig;

    private static Integer SERVER_PORT = getServerPort();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SAVE_COPIES = getSaveCopies();
    private static final Integer NUMBER_OF_LOGS = getNumberOfLogs();
    private static final Integer LOG_SIZE = getLogSize();
    private static final String KAFKA_SERVER_HOST = getKafkaHost();
    private static final Integer NUMBER_OF_NODES = getNumberOfNodes();

    private static final String AUCTIONS_FILE_NAME_PATTERN = "server-%d.json";
    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String LOGS_FILE_NAME_PATTERN = "logs%d.json";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";
    private static final String SNAPSHOT_FILE_NAME_PATTERN = "snapshot%d.json";
    private static final String AUCTION_TOPIC_PATTERN = "auctions-topic-%d";
    private static final String NODE_DIRECTORY_PATTERN = "node%d";

    public AuctionServiceImpl(ServerConfig serverConfig) {
        super();
        this.serverConfig = serverConfig;
    }

    public AuctionServiceImpl() {
    }

    @Override
    public void getAuctions(GetAuctionsRequest getAuctionsRequest,
            StreamObserver<GetAuctionsResponse> responseObserver) {
        List<Auction> auctions = new ArrayList<>();
//        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());
//        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

//        BidiMap<Integer, AuctionServiceBlockingStub> serversStubs = buildStubsMap(serverConfigs.getPort());
//        serversStubs.forEach((port, stub) -> {
//            GetAuctionsResponse response = stub.getAuctions(buildGetAuctionsRequest());
//            auctions.addAll(response.getAuctionsList());
//        });

        serverConfig.getHashTable().forEach((key, value) -> {
            GetLocalAuctionsResponse response = getServerAuctions(key);
            auctions.addAll(response.getAuctionsList());
        });

        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    private GetLocalAuctionsResponse getServerAuctions(String key) {
        ManagedChannel channel = buildChannel(getServerHost(), SERVER_PORT + valueOf(key));
        AuctionServiceBlockingStub stub = buildAuctionServerStub(channel);
        GetLocalAuctionsResponse response = stub.getLocalAuctions(buildGetLocalAuctionsRequest(key));
        channel.shutdown();

        return response;
    }

    private GetLocalAuctionsRequest buildGetLocalAuctionsRequest(String key) {
        return GetLocalAuctionsRequest.newBuilder().setHashTableId(valueOf(key)).build();
    }

    @Override
    public void getLocalAuctions(GetLocalAuctionsRequest getLocalAuctionsRequest,
            StreamObserver<GetLocalAuctionsResponse> responseObserver) {

        AuctionMapper mapper = new AuctionMapper();

        List<AuctionData> auctionsData = loadAuctions(serverConfig);
        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        GetLocalAuctionsResponse response = GetLocalAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sendBid(SendBidRequest sendBidRequest, StreamObserver<SendBidResponse> responseObserver) {
        AtomicReference<SaveBidResponse> response = new AtomicReference<>();

        String id = sendBidRequest.getId();
        serverConfig.getHashTable().forEach((key, value) -> {
            if (isNull(response.get())) {
                saveBidIfServerHasId(sendBidRequest, response, id, Integer.valueOf(key), value, getHashEnd(key));
            }
        });


//        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());

//        new Thread(() -> saveLogs(SAVE_BID, buildSendBidLog(sendBidRequest), serverConfigs.getPort(), auctionsData)).start();/

//
//        successes.add(updateBidIfPresentLocally(auctionsData, sendBidRequest.getAuctionId(), sendBidRequest.getBid(),
//                sendBidRequest.getPort(), sendBidRequest.getUsername()));
//
//        if (!sendBidRequest.getIsServer()) {
//            stubsIdsMap = getServersIdsMap(sendBidRequest.getPort());
//            stubsIdsMap.forEach((stub, ids) -> {
//                if (ids.getIdsInServer().contains(sendBidRequest.getAuctionId())) {
//                    SendBidResponse response = stub.sendBid(buildSendBidRequest(ids.getPort(), sendBidRequest.getBid(),
//                            sendBidRequest.getAuctionId(), sendBidRequest.getUsername()));
//                    successes.add(response.getSuccess());
//                }
//            });
//        }

        SendBidResponse sendBidResponse = SendBidResponse.newBuilder()
                .setSuccess(response.get().getSuccess())
                .build();

        responseObserver.onNext(sendBidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void saveBid(SaveBidRequest saveBidRequest, StreamObserver<SaveBidResponse> responseObserver) {

        AtomicReference<Boolean> success = new AtomicReference<>(FALSE);
        List<AuctionData> auctionsData = loadAuctions(serverConfig);
        Optional<AuctionData> auction = getAuctionById(auctionsData, saveBidRequest.getAuctionId());

//        if (!saveBidRequest.getProcessingLogs()) {
//            saveLogs(SAVE_BID, buildSaveBidLog(saveBidRequest), saveBidRequest.getHashTableId(), auctionsData);
//        }

        auction.ifPresent(auctionData -> {
            if (isValidBid(auctionData.getCurrentBidInfo().getValue(), saveBidRequest.getBid())) {
                auctionData.getCurrentBidInfo().setValue(saveBidRequest.getBid());
                auctionData.getCurrentBidInfo().setUsername(saveBidRequest.getUsername());
                saveAuctions(auctionsData, serverConfig);
                success.set(TRUE);
            } else {
                success.set(FALSE);
            }
        });

        SaveBidResponse saveBidResponse = buildSaveBidResponse(success.get());

        responseObserver.onNext(saveBidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void createAuction(CreateAuctionRequest createAuctionRequest,
            StreamObserver<CreateAuctionResponse> responseObserver) {
        AtomicReference<SaveAuctionResponse> response = new AtomicReference<>();
        String id = createAuctionRequest.getAuction().getId();

        if (isNull(id)) {
            id = generateSha1Hash(createAuctionRequest.getAuction().toString());
        }

        String finalId = id;
        serverConfig.getHashTable().forEach((key, value) -> {
            if (isNull(response.get())) {
                saveAuctionIfServerHasId(createAuctionRequest, response, finalId, Integer.valueOf(key), value,
                        getHashEnd(key));
            }
        });

        CreateAuctionResponse createAuctionResponse =
                buildCreateAuctionResponse(buildAuctionWithId(createAuctionRequest.getAuction(), finalId),
                        response.get().getSuccess());

        responseObserver.onNext(createAuctionResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void saveAuction(SaveAuctionRequest request, StreamObserver<SaveAuctionResponse> responseObserver) {
        AuctionMapper auctionMapper = new AuctionMapper();
        SaveAuctionResponse response;

//        if (!request.getProcessingLogs()) {
//            saveLogs(SAVE_AUCTION, buildSaveAuctionLog(request, auctionMapper, request.getAuctionId()),
//                    request.getServerSufix(), null);
//        }

        try {
            List<AuctionData> auctionList = loadAuctions(serverConfig);
            auctionList.add(auctionMapper.auctionDataFromAuction(request.getAuction()));
            saveAuctions(auctionList, serverConfig);
            response = buildSaveAuctionResponse(TRUE);
        } catch (Exception e) {
            response = buildSaveAuctionResponse(FALSE);
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private SaveAuctionResponse buildSaveAuctionResponse(Boolean success) {
        return SaveAuctionResponse.newBuilder()
                .setSuccess(success)
                .build();
    }

    private void saveBidIfServerHasId(SendBidRequest sendBidRequest, AtomicReference<SaveBidResponse> response,
            String auctionId, Integer key, String init, String end) {
        if (auctionId.compareTo(init) >= 0 && auctionId.compareTo(end) <= 0) {
            response.set(saveBidInThisServer(sendBidRequest, key));
        }
    }

    private void saveAuctionIfServerHasId(CreateAuctionRequest createAuctionRequest,
            AtomicReference<SaveAuctionResponse> response,
            String auctionId, Integer hashIndex, String init, String end) {
        if (auctionId.compareTo(init) >= 0 && auctionId.compareTo(end) <= 0) {
            response.set(publishSaveAuctionMessage(createAuctionRequest, hashIndex, auctionId));
        }
    }

    private SaveBidResponse buildSaveBidResponse(Boolean success) {
        return SaveBidResponse.newBuilder()
                .setSuccess(success)
                .build();
    }

    public SaveBidResponse saveBidInThisServer(SendBidRequest sendBidRequest, Integer hashTableId) {
        ManagedChannel channel = buildChannel(getServerHost(), SERVER_PORT + hashTableId);
        AuctionServiceBlockingStub stub = buildAuctionServerStub(channel);
        SaveBidResponse response = stub.saveBid(buildSaveBidRequest(sendBidRequest, hashTableId));
        channel.shutdown();

        return response;
    }

    public SaveAuctionResponse publishSaveAuctionMessage(CreateAuctionRequest createAuctionRequest,
            Integer hashIndex, String auctionId) {
        ObjectMapper om = new ObjectMapper();
        GrpcRequestAndResponseMapper grpcRequestAndResponseMapper = new GrpcRequestAndResponseMapper();
        SaveAuctionResponse response;
        AuctionProducer producer = new AuctionProducer(KAFKA_SERVER_HOST);

        SaveAuctionRequest request = buildSaveAuctionRequest(createAuctionRequest, hashIndex, auctionId);
        SaveAuctionRequestData requestData =
                grpcRequestAndResponseMapper.saveAuctionRequestDataFromSaveAuctionRequest(request);


        try {
            producer.put(format(AUCTION_TOPIC_PATTERN, hashIndex), SAVE_AUCTION.name(),
                    om.writeValueAsString(requestData));
            producer.close();
            response = buildSaveAuctionResponse(TRUE);
        } catch (Exception e) {
            System.out.println("Error publishing Save Auction message.");
            response = buildSaveAuctionResponse(FALSE);
        }

        return response;
    }

    public SaveAuctionResponse saveAuctionInThisServer(CreateAuctionRequest createAuctionRequest, Integer hashTableId,
            String auctionId) {
        ManagedChannel channel = buildChannel(SERVER_HOST, SERVER_PORT + hashTableId);
        AuctionServiceBlockingStub stub = buildAuctionServerStub(channel);
        SaveAuctionResponse response = stub.saveAuction(
                buildSaveAuctionRequest(createAuctionRequest, hashTableId, auctionId));
        channel.shutdown();

        return response;
    }

    private SaveAuctionRequest buildSaveAuctionRequest(CreateAuctionRequest createAuctionRequest, Integer hashTableId,
            String auctionId) {
        return SaveAuctionRequest.newBuilder()
                .setAuction(buildAuctionWithId(createAuctionRequest.getAuction(), auctionId))
                .setAuctionId(auctionId)
                .build();
    }

    private Auction buildAuctionWithId(Auction auction, String auctionId) {
        return Auction.newBuilder()
                .setId(auctionId)
                .setOwner(auction.getOwner())
                .setProduct(auction.getProduct())
                .setInitialValue(auction.getInitialValue())
                .setCurrentBidInfo(auction.getCurrentBidInfo())
                .setFinishTime(auction.getFinishTime())
                .build();
    }

    private String generateSha1Hash(String dataToDigest) {
        return sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
    }

    private SaveBidRequest buildSaveBidRequest(SendBidRequest sendBidRequest, Integer hashTableId) {
        return SaveBidRequest.newBuilder()
                .setBid(sendBidRequest.getBid())
                .setAuctionId(sendBidRequest.getId())
                .setUsername(sendBidRequest.getUsername())
                .setHashTableId(hashTableId)
                .build();
    }

    private CreateAuctionResponse buildCreateAuctionResponse(Auction finalAuction, Boolean isSuccess) {
        return CreateAuctionResponse.newBuilder()
                .setAuction(finalAuction)
                .setSuccess(isSuccess)
                .build();
    }

//    private LogData buildSaveAuctionLog(SaveAuctionRequest saveAuctionRequest,
//                                        AuctionMapper auctionMapper, String auctionId) {
//        SaveAuctionLog createLog = new SaveAuctionLog(saveAuctionRequest.getHashTableId(), auctionId);
//
//        createLog.setAuction(auctionMapper.auctionDataFromAuction(saveAuctionRequest.getAuction()));
//        createLog.getAuction().setId(auctionId);
//
//        return new LogData(createLog);
//    }

//    private LogData buildSaveBidLog(SaveBidRequest sendBidRequest) {
//        SaveBidLog saveBidLog = new SaveBidLog();
//
//        saveBidLog.setBid(sendBidRequest.getBid());
//        saveBidLog.setAuctionId(sendBidRequest.getAuctionId());
//        saveBidLog.setUsername(sendBidRequest.getUsername());
//        saveBidLog.setHashTableId(sendBidRequest.getHashTableId());
//
//        return new LogData(saveBidLog);
//    }

//    List<AuctionData> loadSnapshot(JsonLoader jsonLoader, Integer sufix) {
//        return new ArrayList<>(jsonLoader.loadList(format(SNAPSHOT_FILE_NAME_PATTERN, sufix), AuctionData.class));
//    }

    private List<AuctionData> loadAuctions(ServerConfig serverConfig) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data/" + format(NODE_DIRECTORY_PATTERN,
                serverConfig.getCurrentNode()));
        return new ArrayList<>(
                jsonLoader.loadList(format(AUCTIONS_FILE_NAME_PATTERN, serverConfig.getCurrentServerPort()),
                        AuctionData.class));
    }

//    private Integer loadAuctionNextId(String resource) {
//        JsonLoader jsonLoader = new JsonLoader("src/main/data");
//        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
//        nextId.setId(nextId.getId() + 1);
//        jsonLoader.saveFile(resource, nextId);
//
//        return nextId.getId() - 1;
//    }

//    NextId loadLogOrSnapshotNextId(String resource, JsonLoader jsonLoader) {
//        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
//        return nextId;
//    }

    public void saveAuctions(List<AuctionData> auctionsToSave, ServerConfig serverConfig) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data/" + format(LOGS_FILE_NAME_PATTERN,
                serverConfig.getCurrentNode()));

        jsonLoader.saveFile(format(AUCTIONS_FILE_NAME_PATTERN, serverConfig.getCurrentServerPort()), auctionsToSave);
    }

//    private void saveLogs(@SuppressWarnings("SameParameterValue") LogFunction function, LogData request,
//            Integer hashTableId, Object data) {
//
//        JsonLoader logsAndSnapshotsLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN,
//                hashTableId));
//
//        NextId nextLog = loadLogOrSnapshotNextId(NEXT_LOG_FILE, logsAndSnapshotsLoader);
//        NextId nextSnapshot = loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, logsAndSnapshotsLoader);
//        List<Log> logs = loadLogs(nextLog.getId(), logsAndSnapshotsLoader);
//        alternateNextIdsAndCreateSnapshotIfLogIsFull(logs, nextLog, nextSnapshot, logsAndSnapshotsLoader, data,
//                hashTableId);
//
//        logs.add(new Log(function, request));
//        logsAndSnapshotsLoader.saveFile(format(LOGS_FILE_NAME_PATTERN, nextLog.getId()), logs);
//
//    }

//    private Object loadDataFromDBIfNecessary(Object data, Integer port) {
//        if (isNull(data)) {
//            data = loadAuctions(port);
//        }
//        return data;
//    }

//    private void createSnapshot(JsonLoader jsonLoader, Object data) {
//        NextId nextSnapshotId = loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, jsonLoader);
//        jsonLoader.saveFile(format(SNAPSHOT_FILE_NAME_PATTERN, nextSnapshotId.getId()), data);
//        jsonLoader.saveFile(NEXT_SNAPSHOT_FILE, nextSnapshotId);
//    }
//
//    private void alternateLogFile(JsonLoader jsonLoader, NextId nextLogId) {
//        setLogsOrSnapshotsNextId(nextLogId);
//        jsonLoader.saveFile(NEXT_LOG_FILE, nextLogId);
//    }
//
//    private void alternateSnapshotFile(JsonLoader jsonLoader, NextId nextLogId) {
//        setLogsOrSnapshotsNextId(nextLogId);
//        jsonLoader.saveFile(NEXT_SNAPSHOT_FILE, nextLogId);
//    }
//
//    private void setLogsOrSnapshotsNextId(NextId nextId) {
//        if (nextId.getId().equals(NUMBER_OF_LOGS - 1)) {
//            nextId.setId(0);
//        } else {
//            nextId.setId(nextId.getId() + 1);
//        }
//    }

//    private void alternateNextIdsAndCreateSnapshotIfLogIsFull(List<Log> logs, NextId nextLog, NextId nextSnapshot,
//            JsonLoader logsAndSnapshotsLoader, Object data, Integer port) {
//        if (logFileIsFull(logs)) {
//            alternateLogFile(logsAndSnapshotsLoader, nextLog);
//            alternateSnapshotFile(logsAndSnapshotsLoader, nextSnapshot);
//            data = loadDataFromDBIfNecessary(data, port);
//            createSnapshot(logsAndSnapshotsLoader, data);
//            logs.clear();
//        }
//    }

//    private boolean logFileIsFull(List<Log> logs) {
//        return logs.size() == LOG_SIZE;
//    }

//    List<Log> loadLogs(Integer id, JsonLoader jsonLoader) {
//        return new ArrayList<>(jsonLoader.loadList(format(LOGS_FILE_NAME_PATTERN, id),
//                Log.class));
//    }

    private Optional<AuctionData> getAuctionById(List<AuctionData> list, String id) {

        return list.stream()
                .filter(auction -> auction.getId().equals(id))
                .findAny();
    }

    private Boolean isValidBid(Double currentBid, Double newBid) {
        return newBid > currentBid;
    }

//    private BidiMap<Integer, AuctionServiceBlockingStub> buildStubsMap(Integer actualServerPort) {
//
//        BidiMap<Integer, AuctionServiceBlockingStub> stubsMap = new DualHashBidiMap<>();
//
//        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
//            if (SERVER_PORT + i != actualServerPort) {
//                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
//                stubsMap.put(SERVER_PORT + i, stub);
//            }
//        }
//
//        return stubsMap;
//    }

//    private BidiMap<Integer, AuctionServiceBlockingStub> buildSavingStubsMap(Integer port) {
//
//        BidiMap<Integer, AuctionServiceBlockingStub> savingStubsMap = new DualHashBidiMap<>();
//
//        for (int i = 1; i < SAVE_COPIES; i++) {
//            if (port + i >= SERVER_PORT + NUMBER_OF_SERVERS) {
//                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT));
//                savingStubsMap.put(SERVER_PORT, stub);
//            } else {
//                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, port + 1));
//                savingStubsMap.put(port + 1, stub);
//            }
//        }
//
//        return savingStubsMap;
//    }


    private String getHashEnd(String currentHash) {
        if (Integer.parseInt(currentHash) >= serverConfig.getHashTable().size() - 1) {
            return "ffff";
        }
        return serverConfig.getHashTable().get(String.valueOf(Integer.parseInt(currentHash) + 1));
    }

    public AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    public ManagedChannel buildChannel(@SuppressWarnings("SameParameterValue") String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public static void main(String[] args) {
//        ClusterManager clusterManager = new ClusterManager();
//
//        ClusterConfig clusterConfig = clusterManager.getClusterConfig();
//
//        clusterConfig.setHashTable(clusterManager.generateHashTable());
//
//        NodeConfig nodeBaseConfig = clusterManager.getNodeBaseConfig(clusterConfig);
//
//        clusterManager.initNodes(nodeBaseConfig);
        Map<String, String> map = new HashMap<>();
        map.put("0", "0");
        map.put("1", "5555");
        map.put("2", "aaaa");
        AuctionServiceImpl auctionService = new AuctionServiceImpl(new ServerConfig(0, 20000, 500, 3, map, 0));

//        auctionService.saveAuction(SaveAuctionRequest.newBuilder()
//                .setAuction(Auction.newBuilder()
//                        .setId("acce")
//                        .setOwner("me")
//                        .setProduct("arroz")
//                        .setInitialValue(1.0)
//                        .setCurrentBidInfo(CurrentBidInfo.newBuilder()
//                                .setValue(1.0)
//                                .setUsername("")
//                                .build())
//                        .setFinishTime(now().plusDays(5).toString())
//                        .build())
//                .setAuctionId("a")
//                .setServerSufix(0)
//                .build(), new StreamObserver<SaveAuctionResponse>() {
//            @Override
//            public void onNext(SaveAuctionResponse saveAuctionResponse) {
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });

        auctionService.createAuction(CreateAuctionRequest.newBuilder()
                .setAuction(Auction.newBuilder()
                        .setId("e")
                        .setOwner("me")
                        .setProduct("arroz")
                        .setInitialValue(1.0)
                        .setCurrentBidInfo(CurrentBidInfo.newBuilder()
                                .setValue(1.0)
                                .setUsername("")
                                .build())
                        .setFinishTime(now().plusDays(5).toString())
                        .build())
                .build(), new StreamObserver<CreateAuctionResponse>() {
            @Override
            public void onNext(CreateAuctionResponse createAuctionResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

//        auctionService.sendBid(SendBidRequest.newBuilder()
//                .setId("acce")
//                .setBid(3.0)
//                .setUsername("eu")
//                .build(), new StreamObserver<SendBidResponse>() {
//            @Override
//            public void onNext(SendBidResponse sendBidResponse) {
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });
//
//        auctionService.saveBid(SaveBidRequest.newBuilder()
//                .setHashTableId(1)
//                .setAuctionId("abcd")
//                .setBid(5.0)
//                .setUsername("eu")
//                .build(), new StreamObserver<SaveBidResponse>() {
//
//            @Override
//            public void onNext(SaveBidResponse saveBidResponse) {
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//
//            }
//
//            @Override
//            public void onCompleted() {
//
//            }
//        });


//        AuctionServiceBlockingStub stub = auctionService.buildAuctionServerStub(forAddress("localhost", 50000)
//                .usePlaintext()
//                .build());
//        stub.sendBid(SendBidRequest.newBuilder()
//                .setAuctionId("abc")
//                .setBid(6.0)
//                .setUsername("eu")
//                .build());

        auctionService.getLocalAuctions(GetLocalAuctionsRequest.newBuilder()
                        .setHashTableId(2)
                        .build()
                , new StreamObserver<GetLocalAuctionsResponse>() {
                    @Override
                    public void onNext(GetLocalAuctionsResponse getLocalAuctionsResponse) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                });

        auctionService.getAuctions(GetAuctionsRequest.newBuilder().build(), new StreamObserver<GetAuctionsResponse>() {
            @Override
            public void onNext(GetAuctionsResponse getAuctionsResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

    }
}
