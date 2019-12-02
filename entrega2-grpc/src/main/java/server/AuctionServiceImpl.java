package server;

import domain.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.AuctionServiceGrpc.AuctionServiceImplBase;
import util.JsonLoader;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.*;

public class AuctionServiceImpl extends AuctionServiceImplBase {

    private ServerConfigs serverConfigs;

    private static Integer SERVER_PORT = getServerPort();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SAVE_COPIES = getSaveCopies();
    private static final Integer NUMBER_OF_LOGS = getNumberOfLogs();
    private static final Integer LOG_SIZE = getLogSize();

    private static final String AUCTIONS_FILE_NAME_PATTERN = "auctions%d.json";
    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String LOGS_FILE_NAME_PATTERN = "logs%d.json";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";
    private static final String SNAPSHOT_FILE_NAME_FORMAT = "snapshot%d.json";

    AuctionServiceImpl() {
    }

    AuctionServiceImpl(ServerConfigs serverConfigs) {
        this.serverConfigs = serverConfigs;
    }

    @Override
    public void auction(AuctionRequest auctionRequest, StreamObserver<AuctionResponse> responseObserver) {

        AuctionResponse response = AuctionResponse.newBuilder()
                .setAction(auctionRequest.getAction())
                .setData(auctionRequest.getData())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAuctions(GetAuctionsRequest getAuctionsRequest,
                            StreamObserver<GetAuctionsResponse> responseObserver) {
        AuctionMapper mapper = new AuctionMapper();

        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());
        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        BidiMap<Integer, AuctionServiceBlockingStub> serversStubs = buildStubsMap(serverConfigs.getPort());
        serversStubs.forEach((port, stub) -> {
            GetAuctionsResponse response = stub.getAuctions(buildGetAuctionsRequest());
            auctions.addAll(response.getAuctionsList());
        });

        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(auctions)
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    //TODO: Fix snapshots
    @Override
    public void sendBid(SendBidRequest sendBidRequest, StreamObserver<SendBidResponse> responseObserver) {
        AtomicReference<SaveBidResponse> response = new AtomicReference<>();

        String id = sendBidRequest.getId();
        Map<String, String> hashTable = generateHashTable();

        hashTable.forEach((key, value) ->  {
            if (nonNull(response.get())) {
                saveBidIfServerHasId(sendBidRequest, response, id, hashTable.size(), Integer.valueOf(key), value)
            }
        });


//        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());

//        new Thread(() -> saveLogs(SEND_BID, buildSendBidLog(sendBidRequest), serverConfigs.getPort(), auctionsData)).start();
//
//        saveLogs(SEND_BID, buildSendBidLog(sendBidRequest), sendBidRequest.getPort(), auctionsData,
//                sendBidRequest.getIsServer(), sendBidRequest.getIsProcessLogs());
//
//        successes.add(updateBidIfPresentLocally(auctionsData, sendBidRequest.getId(), sendBidRequest.getBid(),
//                sendBidRequest.getPort(), sendBidRequest.getUsername()));
//
//        if (!sendBidRequest.getIsServer()) {
//            stubsIdsMap = getServersIdsMap(sendBidRequest.getPort());
//            stubsIdsMap.forEach((stub, ids) -> {
//                if (ids.getIdsInServer().contains(sendBidRequest.getId())) {
//                    SendBidResponse response = stub.sendBid(buildSendBidRequest(ids.getPort(), sendBidRequest.getBid(),
//                            sendBidRequest.getId(), sendBidRequest.getUsername()));
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
        List<AuctionData> auctionsData = loadAuctions(saveBidRequest.getHashTableId());
        Optional<AuctionData> auction = getAuctionById(auctionsData, saveBidRequest.getId());

        auction.ifPresent(auctionData -> {
            auctionData.getCurrentBidInfo().setValue(saveBidRequest.getBid());
            auctionData.getCurrentBidInfo().setUsername(saveBidRequest.getUsername());
            saveAuctions(auctionsData, saveBidRequest.getHashTableId());
            success.set(TRUE);
        });

        SaveBidResponse saveBidResponse = buildSaveBidResponse(success.get());

        responseObserver.onNext(saveBidResponse);
        responseObserver.onCompleted();
    }

    //TODO: Fix snapshots
    @Override
    public void createAuction(CreateAuctionRequest createAuctionRequest,
                              StreamObserver<CreateAuctionResponse> responseObserver) {
        AtomicReference<SaveAuctionResponse> response = new AtomicReference<>();
        String id = createAuctionRequest.getAuction().getId();
        Map<String, String> hashTable = generateHashTable();

        if (isNull(id)) {
            id = generateSha1Hash(createAuctionRequest.getAuction().toString());
        }

        String finalId = id;
        hashTable.forEach((key, value) -> {
            if (nonNull(response.get())) {
                saveAuctionIfServerHasId(createAuctionRequest, response, finalId, hashTable.size(), Integer.valueOf(key), value);
            }
        });


//        new Thread(() -> saveLogs(CREATE_AUCTION, buildCreateAuctionLog(createAuctionRequest, auctionMapper, nextId),
//                createAuctionRequest.getPort(), null, createAuctionRequest.getIsServer(),
//                createAuctionRequest.getIsProcessLogs()));

//        saveLogs(CREATE_AUCTION, buildCreateAuctionLog(createAuctionRequest, auctionMapper, nextId),
//                createAuctionRequest.getPort(), null, createAuctionRequest.getIsServer(), createAuctionRequest.getIsProcessLogs());


        CreateAuctionResponse createAuctionResponse =
                buildCreateAuctionResponse(createAuctionRequest.getAuction(), response.get().getSuccess());

        responseObserver.onNext(createAuctionResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void saveAuction(SaveAuctionRequest request, StreamObserver<SaveAuctionResponse> responseObserver) {
        AuctionMapper auctionMapper = new AuctionMapper();
        SaveAuctionResponse response;

        try {
            List<AuctionData> auctionList = loadAuctions(request.getHashTableId());
            auctionList.add(auctionMapper.auctionDataFromAuction(request.getAuction()));
            saveAuctions(auctionList, request.getHashTableId());
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

    @Override
    public void getAuctionsIds(GetAuctionsIdsRequest getAuctionsIdsRequest,
                               StreamObserver<GetAuctionsIdsResponse> responseObserver) {

        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());
        List<String> ids = auctionsData.stream()
                .map(AuctionData::getId)
                .collect(Collectors.toList());

        GetAuctionsIdsResponse response = GetAuctionsIdsResponse.newBuilder()
                .addAllIds(ids)
                .build();

        System.out.println(response.getIdsList());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void saveBidIfServerHasId(SendBidRequest sendBidRequest, AtomicReference<SaveBidResponse> response,
                                      String id, Integer hashTableSize, Integer key, String serverHash) {
        if (id.compareTo(serverHash) < 0 ) {
            response.set(saveBidInThisServer(sendBidRequest, key));
        } else if (key.equals(hashTableSize - 1)) {
            response.set(saveBidInThisServer(sendBidRequest, 0));
        }
    }

    private void saveAuctionIfServerHasId(CreateAuctionRequest createAuctionRequest, AtomicReference<SaveAuctionResponse> response,
                                          String id, Integer hashTableSize, Integer key, String serverHash) {
        if (id.compareTo(serverHash) < 0) {
            response.set(saveAuctionInThisServer(createAuctionRequest, key));
        } else if (key.equals(hashTableSize - 1)) {
            response.set(saveAuctionInThisServer(createAuctionRequest, 0));
        }
    }

    private SaveBidResponse buildSaveBidResponse(Boolean success) {
        return SaveBidResponse.newBuilder()
                .setSuccess(success)
                .build();
    }

    private SaveBidResponse saveBidInThisServer(SendBidRequest sendBidRequest, Integer hashTableId) {
        ManagedChannel channel = buildChannel(getServerHost(), SERVER_PORT + hashTableId);
        AuctionServiceBlockingStub stub = buildAuctionServerStub(channel);
        SaveBidResponse response = stub.saveBid(buildSaveBidRequest(sendBidRequest, hashTableId));
        channel.shutdown();

        return response;
    }

    private SaveAuctionResponse saveAuctionInThisServer(CreateAuctionRequest createAuctionRequest, Integer hashTableId) {
        ManagedChannel channel = buildChannel(getServerHost(), SERVER_PORT + hashTableId);
        AuctionServiceBlockingStub stub = buildAuctionServerStub(channel);
        SaveAuctionResponse response = stub.saveAuction(buildSaveAuctionRequest(createAuctionRequest, hashTableId));
        channel.shutdown();

        return response;
    }

    private SaveAuctionRequest buildSaveAuctionRequest(CreateAuctionRequest createAuctionRequest, Integer hashTableId) {
        return SaveAuctionRequest.newBuilder()
                .setAuction(createAuctionRequest.getAuction())
                .setHashTableId(hashTableId)
                .build();
    }

    private String generateSha1Hash(String dataToDigest) {
        return sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private Integer findServerPortToStoreData(String hash) {
        Map<String, String> hashTable = serverConfigs.getHashTable();

        for (int i = 0; i < hashTable.size(); i++) {
            int nextServer = i + 1;

            if (i == hashTable.size() - 1) {
                nextServer = 0;
            }

            if (hashTable.get(i).compareTo(hash) <= 0 && hashTable.get(nextServer).compareTo(hash) > 0) {
                return i + serverConfigs.getPort();
            }
        }
        return serverConfigs.getPort();
    }

    private SaveBidRequest buildSaveBidRequest(SendBidRequest sendBidRequest, Integer hashTableId) {
        return SaveBidRequest.newBuilder()
                .setBid(sendBidRequest.getBid())
                .setId(sendBidRequest.getId())
                .setUsername(sendBidRequest.getUsername())
                .setHashTableId(hashTableId)
                .build();
    }

    private Map<String, String> generateHashTable() {
        List<String> list = generateIdList();

        Map<String, String> hashTable = new HashMap<>();
        list.forEach(obj -> hashTable.put(String.valueOf(hashTable.size()), obj));

        return hashTable;
    }

    private List<String> generateIdList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            list.add(generateSha1Hash(String.valueOf(i)));
        }
        list.sort(Comparator.comparing(String::toLowerCase));
        return list;
    }

    private List<Auction> removeRepeatedDataFromAuctionList(List<Auction> auctions) {
        return auctions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<AuctionData> removeRepeatedDataFromAuctionDataList(List<AuctionData> auctions) {
        return auctions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private GetAuctionsRequest buildGetAuctionsRequest() {
        return GetAuctionsRequest.newBuilder().build();
    }


    private CreateAuctionResponse buildCreateAuctionResponse(Auction finalAuction, Boolean isSuccess) {
        return CreateAuctionResponse.newBuilder()
                .setAuction(finalAuction)
                .setSuccess(isSuccess)
                .build();
    }

    private Boolean saveAuctionsLocally(Integer port, AuctionData auctionToSave) {
        List<AuctionData> auctions = loadAuctions(port);
        auctions.add(auctionToSave);
        saveAuctions(removeRepeatedDataFromAuctionDataList(auctions), port);

        return TRUE;
    }

    private boolean saveAuctionInOtherServers(AuctionData auctionData, Integer originPort) {
        AuctionMapper auctionMapper = new AuctionMapper();

        Auction auctionToSave = auctionMapper.auctionFromAuctionData(auctionData);

        AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, originPort));
        CreateAuctionResponse response = stub.createAuction(buildCreateAuctionRequest(auctionToSave));

        return response.getSuccess();
    }

    private CreateAuctionRequest buildCreateAuctionRequest(Auction auction) {
        return CreateAuctionRequest.newBuilder()
                .setAuction(auction)
                .build();
    }

    private CreateAuctionRequest buildCreateAuctionRequest(CreateAuctionRequest createAuctionRequest) {
        return CreateAuctionRequest.newBuilder()
                .setAuction(createAuctionRequest.getAuction())
                .build();
    }

    private LogData buildCreateAuctionLog(CreateAuctionRequest createAuctionRequest,
                                          AuctionMapper auctionMapper, String nextId) {
        CreateAuctionLog createLog = new CreateAuctionLog();

        createLog.setAuction(auctionMapper.auctionDataFromAuction(createAuctionRequest.getAuction()));
        createLog.getAuction().setId(nextId);

        return new LogData(createLog);
    }

    private LogData buildSendBidLog(SendBidRequest sendBidRequest) {
        SendBidLog sendBidLog = new SendBidLog();

        sendBidLog.setBid(sendBidRequest.getBid());
        sendBidLog.setId(sendBidRequest.getId());
        sendBidLog.setUsername(sendBidRequest.getUsername());

        return new LogData(sendBidLog);
    }

    private boolean isSuccessfulUpdate(List<Boolean> successes) {
        return notContainsAnyFalse(successes) && !isNullElementsList(successes);
    }

    private boolean isNullElementsList(List<Boolean> successes) {
        return successes.stream().allMatch(Objects::isNull);
    }

    private boolean notContainsAnyFalse(List<Boolean> successes) {
        return !successes.contains(FALSE);
    }

    private SendBidRequest buildSendBidRequest(Double bid, String id, String username) {
        return SendBidRequest.newBuilder()
                .setBid(bid)
                .setId(id)
                .setUsername(username)
                .build();
    }

//    private BidiMap<AuctionServiceBlockingStub, ServerInfo> getServersIdsMap(
//            Integer port) {
//        BidiMap<Integer, AuctionServiceBlockingStub> stubs = buildStubsMap(port);
//        BidiMap<AuctionServiceBlockingStub, ServerInfo> stubsIdsMap = new DualHashBidiMap<>();
//
//        stubs.forEach((serverPort, stub) -> {
//            List<Integer> stubIds = stub.getAuctionsIds(buildGetAuctionIdsRequest(serverPort)).getIdsList();
//            stubsIdsMap.put(stub, new ServerInfo(serverPort, stubIds));
//        });
//        return stubsIdsMap;
//    }

    private static GetAuctionsIdsRequest buildGetAuctionIdsRequest() {
        return GetAuctionsIdsRequest.newBuilder().build();
    }

//    private Boolean updateBidIfPresentLocally(List<AuctionData> auctionsData, String id, Double newBid,
//                                              Integer port, String username) {
//
//        Boolean success = null;
//        AuctionData auctionToChange = getAuctionById(auctionsData, id);
//
//        if (auctionToChange != null) {
//            success = FALSE;
//            if (isValidBid(auctionToChange.getCurrentBidInfo().getValue(), newBid)) {
//                auctionToChange.getCurrentBidInfo().setValue(newBid);
//                auctionToChange.getCurrentBidInfo().setUsername(username);
//                saveAuctions(auctionsData, port);
//                success = TRUE;
//            }
//        }
//        return success;
//    }

    List<AuctionData> loadSnapshot(JsonLoader jsonLoader, Integer sufix) {
        return new ArrayList<>(jsonLoader.loadList(format(SNAPSHOT_FILE_NAME_FORMAT, sufix), AuctionData.class));
    }

    private List<AuctionData> loadAuctions(Integer hashTableId) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        return new ArrayList<>(jsonLoader.loadList(String.format(AUCTIONS_FILE_NAME_PATTERN, hashTableId),
                AuctionData.class));
    }

    private Integer loadAuctionNextId(String resource) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
        nextId.setId(nextId.getId() + 1);
        jsonLoader.saveFile(resource, nextId);

        return nextId.getId() - 1;
    }

    NextId loadLogOrSnapshotNextId(String resource, JsonLoader jsonLoader) {
        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
        return nextId;
    }

    private void saveAuctions(List<AuctionData> auctionsToSave, Integer hashTableId) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        jsonLoader.saveFile(format(AUCTIONS_FILE_NAME_PATTERN, hashTableId), auctionsToSave);
    }

    private void saveLogs(@SuppressWarnings("SameParameterValue") LogFunctions function, LogData request, Integer port, Object data) {

        JsonLoader logsAndSnapshotsLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN,
                port - SERVER_PORT));

        NextId nextLog = loadLogOrSnapshotNextId(NEXT_LOG_FILE, logsAndSnapshotsLoader);
        NextId nextSnapshot = loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, logsAndSnapshotsLoader);
        List<Log> logs = loadLogs(nextLog.getId(), logsAndSnapshotsLoader);
        alternateNextIdsAndCreateSnapshotIfLogIsFull(logs, nextLog, nextSnapshot, logsAndSnapshotsLoader, data,
                port);

        logs.add(new Log(function, request));
        logsAndSnapshotsLoader.saveFile(format(LOGS_FILE_NAME_PATTERN, nextLog.getId()), logs);

    }

    private Object loadDataFromDBIfNecessary(Object data, Integer port) {
        if (isNull(data)) {
            data = loadAuctions(port);
        }
        return data;
    }

    private void createSnapshot(JsonLoader jsonLoader, Object data) {
        NextId nextSnapshotId = loadLogOrSnapshotNextId(NEXT_SNAPSHOT_FILE, jsonLoader);
        jsonLoader.saveFile(format(SNAPSHOT_FILE_NAME_FORMAT, nextSnapshotId.getId()), data);
        jsonLoader.saveFile(NEXT_SNAPSHOT_FILE, nextSnapshotId);
    }

    private void alternateLogFile(JsonLoader jsonLoader, NextId nextLogId) {
        setLogsOrSnapshotsNextId(nextLogId);
        jsonLoader.saveFile(NEXT_LOG_FILE, nextLogId);
    }

    private void alternateSnapshotFile(JsonLoader jsonLoader, NextId nextLogId) {
        setLogsOrSnapshotsNextId(nextLogId);
        jsonLoader.saveFile(NEXT_SNAPSHOT_FILE, nextLogId);
    }

    private void setLogsOrSnapshotsNextId(NextId nextId) {
        if (nextId.getId().equals(NUMBER_OF_LOGS - 1)) {
            nextId.setId(0);
        } else {
            nextId.setId(nextId.getId() + 1);
        }
    }

    private void alternateNextIdsAndCreateSnapshotIfLogIsFull(List<Log> logs, NextId nextLog, NextId nextSnapshot,
                                                              JsonLoader logsAndSnapshotsLoader, Object data, Integer port) {
        if (logFileIsFull(logs)) {
            alternateLogFile(logsAndSnapshotsLoader, nextLog);
            alternateSnapshotFile(logsAndSnapshotsLoader, nextSnapshot);
            data = loadDataFromDBIfNecessary(data, port);
            createSnapshot(logsAndSnapshotsLoader, data);
            logs.clear();
        }
    }

    private boolean logFileIsFull(List<Log> logs) {
        return logs.size() == LOG_SIZE;
    }

    List<Log> loadLogs(Integer id, JsonLoader jsonLoader) {
        return new ArrayList<>(jsonLoader.loadList(format(LOGS_FILE_NAME_PATTERN, id),
                Log.class));
    }

    private Optional<AuctionData> getAuctionById(List<AuctionData> list, String id) {

        return list.stream()
                .filter(auction -> auction.getId().equals(id))
                .findAny();
    }

    private Boolean isValidBid(Double currentBid, Double newBid) {
        return newBid > currentBid;
    }

    private BidiMap<Integer, AuctionServiceBlockingStub> buildStubsMap(Integer actualServerPort) {

        BidiMap<Integer, AuctionServiceBlockingStub> stubsMap = new DualHashBidiMap<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            if (SERVER_PORT + i != actualServerPort) {
                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
                stubsMap.put(SERVER_PORT + i, stub);
            }
        }

        return stubsMap;
    }

    @SuppressWarnings("unused")
    private BidiMap<Integer, AuctionServiceBlockingStub> buildSavingStubsMap(Integer port) {

        BidiMap<Integer, AuctionServiceBlockingStub> savingStubsMap = new DualHashBidiMap<>();

        for (int i = 1; i < SAVE_COPIES; i++) {
            if (port + i >= SERVER_PORT + NUMBER_OF_SERVERS) {
                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT));
                savingStubsMap.put(SERVER_PORT, stub);
            } else {
                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, port + 1));
                savingStubsMap.put(port + 1, stub);
            }
        }

        return savingStubsMap;
    }

    private AuctionServiceBlockingStub buildAuctionServerStub(ManagedChannel channel) {
        return newBlockingStub(channel);
    }

    private ManagedChannel buildChannel(@SuppressWarnings("SameParameterValue") String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public static void main(String[] args) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

        auctionService.sendBid(SendBidRequest.newBuilder()
                .setId("1")
                .setBid(5.0)
                .setUsername("eu")
                .build(), new StreamObserver<SendBidResponse>() {
            @Override
            public void onNext(SendBidResponse sendBidResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

//        auctionService.saveBid(SaveBidRequest.newBuilder()
//                .setHashTableId(0)
//                .setId("abc")
//                .setBid(3.0)
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

//        auctionService.saveAuction(SaveAuctionRequest.newBuilder()
//                .setAuction(Auction.newBuilder()
//                        .setId("a")
//                        .setOwner("me")
//                        .setProduct("arroz")
//                        .setInitialValue(1.0)
//                        .setCurrentBidInfo(CurrentBidInfo.newBuilder()
//                                .setValue(1.0)
//                                .setUsername("")
//                                .build())
//                        .setFinishTime(now().plusDays(5).toString())
//                        .build())
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

//        auctionService.createAuction(CreateAuctionRequest.newBuilder()
//                .setAuction(Auction.newBuilder()
//                        .setId("c")
//                        .setOwner("me")
//                        .setProduct("arroz")
//                        .setInitialValue(1.0)
//                        .setCurrentBidInfo(CurrentBidInfo.newBuilder()
//                                .setValue(1.0)
//                                .setUsername("")
//                                .build())
//                        .setFinishTime(now().plusDays(5).toString())
//                        .build())
//                .build(), new StreamObserver<CreateAuctionResponse>() {
//            @Override
//            public void onNext(CreateAuctionResponse createAuctionResponse) {
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
//                .setId("abc")
//                .setBid(6.0)
//                .setUsername("eu")
//                .build());


    }
}
