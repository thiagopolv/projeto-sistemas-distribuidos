package server;

import domain.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import org.apache.commons.codec.digest.DigestUtils;
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
import static java.util.Objects.isNull;
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
        List<Boolean> successes = new ArrayList<>();
        AtomicReference<SaveBidResponse> response = new AtomicReference<>();
        BidiMap<AuctionServiceBlockingStub, ServerInfo> stubsIdsMap;

        String id = sendBidRequest.getId();
        Map<String, String> hashTable = generateHashTable();

        hashTable.entrySet().forEach(data -> {
            if (id.compareTo(data.getValue()) > 0 || data.getValue().equals(hashTable.size() - 1)) {
                response.set(saveBidInThisServer(sendBidRequest, parseInt(data.getKey())));
            }
        });

        List<AuctionData> auctionsData = loadAuctions(serverConfigs.getPort());

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

        System.out.println(sendBidResponse.getSuccess());
        responseObserver.onNext(sendBidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void saveBid(SaveBidRequest saveBidRequest, StreamObserver<SaveBidResponse> responseObserver) {

        List<AuctionData> auctionsData = loadAuctions(SERVER_PORT + saveBidRequest.getHashTableId());
        Optional<AuctionData> auction = getAuctionById(auctionsData, saveBidRequest.getId());

        auction.ifPresent(auctionData -> {
            auctionData.getCurrentBidInfo().setValue(saveBidRequest.getBid());
            auctionData.getCurrentBidInfo().setUsername(saveBidRequest.getUsername());
        });

        SaveBidResponse saveBidResponse = buildSaveBidResponse(auction);

        responseObserver.onNext(saveBidResponse);
        responseObserver.onCompleted();
    }

    private SaveBidResponse buildSaveBidResponse(Optional<AuctionData> auction) {
        return SaveBidResponse.newBuilder()
                    .setSuccess(auction.isPresent())
                    .build();
    }

    private SaveBidResponse saveBidInThisServer(SendBidRequest sendBidRequest, Integer hashTableId) {
        AuctionServiceBlockingStub stub =
                buildAuctionServerStub(buildChannel(getServerHost(), SERVER_PORT + hashTableId));
        return stub.saveBid(buildSaveBidRequest(sendBidRequest, hashTableId));
    }

    //TODO: Fix snapshots
    @Override
    public void createAuction(CreateAuctionRequest createAuctionRequest,
                              StreamObserver<CreateAuctionResponse> responseObserver) {
        AuctionMapper auctionMapper = new AuctionMapper();
        Boolean isSuccess;
        String hashId = createAuctionRequest.getAuction().getId();

        if (isNull(hashId)) {
            hashId = generateSha1Hash(createAuctionRequest.getAuction().toString());
        }

        Integer serverPortToSave = findServerPortToStoreData(hashId);

//        new Thread(() -> saveLogs(CREATE_AUCTION, buildCreateAuctionLog(createAuctionRequest, auctionMapper, nextId),
//                createAuctionRequest.getPort(), null, createAuctionRequest.getIsServer(),
//                createAuctionRequest.getIsProcessLogs()));

//        saveLogs(CREATE_AUCTION, buildCreateAuctionLog(createAuctionRequest, auctionMapper, nextId),
//                createAuctionRequest.getPort(), null, createAuctionRequest.getIsServer(), createAuctionRequest.getIsProcessLogs());

        Auction auction = createAuctionRequest.getAuction();
        AuctionData auctionToSave = auctionMapper.auctionDataFromAuction(auction);
        auctionToSave.setId(hashId);

        if (serverPortToSave.equals(serverConfigs.getPort())) {
            isSuccess = saveAuctionsLocally(serverPortToSave, auctionToSave);
        } else {
            isSuccess = saveAuctionInOtherServers(auctionToSave, serverPortToSave);
        }
        Auction finalAuction = auctionMapper.auctionFromAuctionData(auctionToSave);

        CreateAuctionResponse response = buildCreateAuctionResponse(finalAuction, isSuccess);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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

//    @Override
//    public void verifyAuctionOnOtherServers(VerifyAuctionRequest verifyAuctionRequest) {
//
//    }

    private String generateSha1Hash(String dataToDigest) {
        return DigestUtils.sha1Hex(dataToDigest.getBytes(StandardCharsets.UTF_8));
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

    private List<AuctionData> loadAuctions(Integer port) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        return new ArrayList<>(jsonLoader.loadList( String.format(AUCTIONS_FILE_NAME_PATTERN, port - SERVER_PORT),
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

    void saveAuctions(List<AuctionData> auctionsToSave, Integer port) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        jsonLoader.saveFile(format(AUCTIONS_FILE_NAME_PATTERN, port - SERVER_PORT), auctionsToSave);
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

    }
}
