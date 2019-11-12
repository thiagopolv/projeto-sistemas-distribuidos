package server;

import static domain.LogFunctions.CREATE_AUCTION;
import static domain.LogFunctions.SEND_BID;
import static io.grpc.ManagedChannelBuilder.forAddress;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static server.AuctionServiceGrpc.newBlockingStub;
import static util.ConfigProperties.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import domain.CreateAuctionLog;
import domain.Log;
import domain.LogData;
import domain.LogFunctions;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import mapper.AuctionData;
import mapper.AuctionMapper;
import domain.NextId;
import server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import server.AuctionServiceGrpc.AuctionServiceImplBase;
import util.JsonLoader;

public class AuctionServiceImpl extends AuctionServiceImplBase {

    private static Integer SERVER_PORT = getServerPort();
    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();
    private static final String SERVER_HOST = getServerHost();
    private static final Integer SAVE_COPIES = getSaveCopies();
    private static final Integer NUMBER_OF_LOGS = getNumberOfLogs();
    private static final Integer LOG_SIZE = getLogSize();

    private static final String NEXT_ID_FILE = "next-id.json";
    private static final String AUCTIONS_FILE_NAME_PATTERN = "auctions%d.json";
    private static final String LOGS_DIR_NAME_PATTERN = "logs-snapshots-%d";
    private static final String LOGS_FILE_NAME_PATTERN = "logs%d.json";
    private static final String NEXT_LOG_FILE = "next-log.json";
    private static final String NEXT_SNAPSHOT_FILE = "next-snapshot.json";
    private static final String SNAPSHOT_FILE_NAME_FORMAT = "snapshot%d.json";

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

        List<AuctionData> auctionsData = loadAuctions(getAuctionsRequest.getPort());
        List<Auction> auctions = mapper.auctionListFromAuctionDataList(auctionsData);

        if (!getAuctionsRequest.getIsServer()) {
            BidiMap<Integer, AuctionServiceBlockingStub> serversStubs = buildStubsMap(getAuctionsRequest.getPort());
            serversStubs.forEach((port, stub) -> {
                GetAuctionsResponse response = stub.getAuctions(buildGetAuctionsRequest(port, TRUE));
                auctions.addAll(response.getAuctionsList());
            });
        }

        System.out.println(auctions);
        GetAuctionsResponse getAuctionsResponse = GetAuctionsResponse.newBuilder()
                .addAllAuctions(removeRepeatedDataFromList(auctions))
                .build();

        responseObserver.onNext(getAuctionsResponse);
        responseObserver.onCompleted();
    }

    private List<Auction> removeRepeatedDataFromList(List<Auction> auctions) {
        return auctions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private GetAuctionsRequest buildGetAuctionsRequest(Integer port, Boolean isServer) {
        return GetAuctionsRequest.newBuilder().setPort(port).setIsServer(isServer).build();
    }

    @Override
    public void sendBid(SendBidRequest sendBidRequest, StreamObserver<SendBidResponse> responseObserver) {
        List<Boolean> successes = new ArrayList<>();
        BidiMap<AuctionServiceBlockingStub, ServerInfo> stubsIdsMap;

        List<AuctionData> auctionsData = loadAuctions(sendBidRequest.getPort());
//        saveLogs(SEND_BID, sendBidRequest, sendBidRequest.getPort(), auctionsData, sendBidRequest.getIsServer());
        successes.add(updateBidIfPresentLocally(auctionsData, sendBidRequest.getId(), sendBidRequest.getBid(),
                sendBidRequest.getPort(), sendBidRequest.getUsername()));

        if (!sendBidRequest.getIsServer()) {
            stubsIdsMap = getServersIdsMap(sendBidRequest.getPort());
            stubsIdsMap.forEach((stub, ids) -> {
                if (ids.getIdsInServer().contains(sendBidRequest.getId())) {
                    SendBidResponse response = stub.sendBid(buildSendBidRequest(ids.getPort(), sendBidRequest.getBid(),
                            sendBidRequest.getId(), sendBidRequest.getUsername()));
                    successes.add(response.getSuccess());
                }
            });
        }

        SendBidResponse sendBidResponse = SendBidResponse.newBuilder()
                .setSuccess(isSuccessfulUpdate(successes))
                .build();

        System.out.println(sendBidResponse.getSuccess());
        responseObserver.onNext(sendBidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void createAuction(CreateAuctionRequest createAuctionRequest,
            StreamObserver<CreateAuctionResponse> responseObserver) {
        AuctionMapper auctionMapper = new AuctionMapper();
        List<Boolean> successes = new ArrayList<>();
        saveLogs(CREATE_AUCTION, buildCreateAuctionLog(createAuctionRequest, auctionMapper),
                createAuctionRequest.getPort(), null, createAuctionRequest.getIsServer());

        Auction auction = createAuctionRequest.getAuction();
        AuctionData auctionToSave = auctionMapper.auctionDataFromAuction(auction);

        if (!createAuctionRequest.getIsServer()) {
            Integer nextId = loadAuctionNextId(NEXT_ID_FILE);
            auctionToSave.setId(nextId);
            successes.addAll(saveAuctionInOtherServers(auctionToSave, createAuctionRequest.getPort()));
        }

        successes.add(saveAuctionsLocally(createAuctionRequest.getPort(), auctionToSave));

        CreateAuctionResponse response = buildCreateAuctionResponse(auctionMapper, successes, auctionToSave);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAuctionsIds(GetAuctionsIdsRequest getAuctionsIdsRequest,
            StreamObserver<GetAuctionsIdsResponse> responseObserver) {

        List<AuctionData> auctionsData = loadAuctions(getAuctionsIdsRequest.getPort());
        List<Integer> ids = auctionsData.stream()
                .map(AuctionData::getId)
                .collect(Collectors.toList());

        GetAuctionsIdsResponse response = GetAuctionsIdsResponse.newBuilder()
                .addAllIds(ids)
                .build();

        System.out.println(response.getIdsList());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private CreateAuctionResponse buildCreateAuctionResponse(AuctionMapper auctionMapper, List<Boolean> successes,
            AuctionData auctionToSave) {
        return CreateAuctionResponse.newBuilder()
                .setAuction(auctionMapper.auctionFromAuctionData(auctionToSave))
                .setSuccess(isSuccessfulCreate(successes))
                .build();
    }

    private Boolean saveAuctionsLocally(Integer port, AuctionData auctionToSave) {
        List<AuctionData> auctions = loadAuctions(port);
        auctions.add(auctionToSave);
        saveAuctions(auctions, port);

        return TRUE;
    }

    private List<Boolean> saveAuctionInOtherServers(AuctionData auctionData, Integer originPort) {
        AuctionMapper auctionMapper = new AuctionMapper();
        List<Boolean> successes = new ArrayList<>();

        Auction auctionToSave = auctionMapper.auctionFromAuctionData(auctionData);
        BidiMap<Integer, AuctionServiceBlockingStub> savingStubsMap = buildSavingStubsMap(originPort);

        savingStubsMap.forEach((port, stub) -> {
            CreateAuctionResponse response = stub.createAuction(buildCreateAuctionRequest(port, auctionToSave));
            successes.add(response.getSuccess());
        });

        return successes;
    }

    private CreateAuctionRequest buildCreateAuctionRequest(Integer port, Auction auction) {
        return CreateAuctionRequest.newBuilder()
                .setPort(port)
                .setAuction(auction)
                .setIsServer(TRUE)
                .build();
    }

    private CreateAuctionRequest buildCreateAuctionRequest(CreateAuctionRequest createAuctionRequest) {
        return CreateAuctionRequest.newBuilder()
                .setPort(createAuctionRequest.getPort())
                .setAuction(createAuctionRequest.getAuction())
                .build();
    }

    private LogData buildCreateAuctionLog(CreateAuctionRequest createAuctionRequest,
            AuctionMapper auctionMapper) {
        CreateAuctionLog createLog = new CreateAuctionLog();

        createLog.setAuction(auctionMapper.auctionDataFromAuction(createAuctionRequest.getAuction()));
        createLog.setPort(createAuctionRequest.getPort());
        createLog.setServer(createAuctionRequest.getIsServer());

        return new LogData(createLog);
    }

    private boolean isSuccessfulUpdate(List<Boolean> successes) {
        return notContainsAnyFalse(successes) && !isNullElementsList(successes);
    }

    private boolean isSuccessfulCreate(List<Boolean> successes) {
        return notContainsAnyFalse(successes) && !isNullElementsList(successes);
    }

    private boolean isNullElementsList(List<Boolean> successes) {
        return successes.stream().allMatch(Objects::isNull);
    }

    private boolean notContainsAnyFalse(List<Boolean> successes) {
        return !successes.contains(FALSE);
    }

    private SendBidRequest buildSendBidRequest(Integer port, Double bid, Integer id, String username) {
        return SendBidRequest.newBuilder()
                .setPort(port)
                .setBid(bid)
                .setId(id)
                .setUsername(username)
                .setIsServer(TRUE)
                .build();
    }

    private BidiMap<AuctionServiceBlockingStub, ServerInfo> getServersIdsMap(
            Integer port) {
        BidiMap<Integer, AuctionServiceBlockingStub> stubs = buildStubsMap(port);
        BidiMap<AuctionServiceBlockingStub, ServerInfo> stubsIdsMap = new DualHashBidiMap<>();

        stubs.forEach((serverPort, stub) -> {
            List<Integer> stubIds = stub.getAuctionsIds(buildGetAuctionIdsRequest(serverPort)).getIdsList();
            stubsIdsMap.put(stub, new ServerInfo(serverPort, stubIds));
        });
        return stubsIdsMap;
    }

    private static GetAuctionsIdsRequest buildGetAuctionIdsRequest(Integer port) {
        return GetAuctionsIdsRequest.newBuilder().setPort(port).build();
    }

    private Boolean updateBidIfPresentLocally(List<AuctionData> auctionsData, Integer id, Double newBid,
            Integer port, String username) {

        Boolean success = null;
        AuctionData auctionToChange = getAuctionById(auctionsData, id);

        if (auctionToChange != null) {
            success = FALSE;
            if (isValidBid(auctionToChange.getCurrentBidInfo().getValue(), newBid)) {
                auctionToChange.getCurrentBidInfo().setValue(newBid);
                auctionToChange.getCurrentBidInfo().setUsername(username);
                saveAuctions(auctionsData, port);
                success = TRUE;
            }
        }
        return success;
    }

    public List<AuctionData> loadSnapshot(JsonLoader jsonLoader, Integer sufix) {
        return new ArrayList<>(jsonLoader.loadList(format(SNAPSHOT_FILE_NAME_FORMAT, sufix) , AuctionData.class));
    }

    private List<AuctionData> loadAuctions(Integer port) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        return new ArrayList<>(jsonLoader.loadList("auctions" + (port - SERVER_PORT) + ".json",
                AuctionData.class));
    }

    private Integer loadAuctionNextId(String resource) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");
        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
        nextId.setId(nextId.getId() + 1);
        jsonLoader.saveFile(resource, nextId);

        return nextId.getId() - 1;
    }

    public NextId loadLogOrSnapshotNextId(String resource, JsonLoader jsonLoader) {
        NextId nextId = jsonLoader.loadObject(resource, NextId.class);
        return nextId;
    }

    public void saveAuctions(List<AuctionData> auctionsToSave, Integer port) {
        JsonLoader jsonLoader = new JsonLoader("src/main/data");

        jsonLoader.saveFile(format(AUCTIONS_FILE_NAME_PATTERN, port - SERVER_PORT), auctionsToSave);
    }

    private void saveLogs(LogFunctions function, LogData request, Integer port, Object data, Boolean isServer) {
        if (isServer) {
            return;
        }

        JsonLoader jsonLoader = new JsonLoader("src/main/data/" + format(LOGS_DIR_NAME_PATTERN, SERVER_PORT - port));

        NextId nextLogId = loadLogOrSnapshotNextId(NEXT_LOG_FILE, jsonLoader);
        List<Log> logs = loadLogs(nextLogId.getId(), jsonLoader);
        validateIfNeedsToClearLogs(logs);

        logs.add(new Log(function, request));
        jsonLoader.saveFile(format(LOGS_FILE_NAME_PATTERN, nextLogId.getId()), logs);

        validateIfNeedsToAlternateLogFileAndCreateSnapshot(jsonLoader, nextLogId, logs, data, port);
    }

    private void validateIfNeedsToAlternateLogFileAndCreateSnapshot(JsonLoader jsonLoader, NextId nextLogId,
            List<Log> logs, Object data, Integer port) {

        if (logFileIsFull(logs)) {
            data = loadDataFromDBIfNecessary(data, port);
            alternateLogFile(jsonLoader, nextLogId);
            createSnapshot(jsonLoader, data);
        }
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
        setLogsAndSnapshotsNextId(nextSnapshotId);
        jsonLoader.saveFile(NEXT_SNAPSHOT_FILE, nextSnapshotId);
    }

    private void alternateLogFile(JsonLoader jsonLoader, NextId nextLogId) {
        setLogsAndSnapshotsNextId(nextLogId);
        jsonLoader.saveFile(NEXT_LOG_FILE, nextLogId);
    }

    private void setLogsAndSnapshotsNextId(NextId nextId) {
        if (nextId.getId().equals(NUMBER_OF_LOGS)) {
            nextId.setId(0);
        } else {
            nextId.setId(nextId.getId() + 1);
        }
    }

    private void validateIfNeedsToClearLogs(List<Log> logs) {
        if (logFileIsFull(logs)) {
            logs.clear();
        }
    }

    private boolean logFileIsFull(List<Log> logs) {
        return logs.size() == LOG_SIZE;
    }

    public List<Log> loadLogs(Integer id, JsonLoader jsonLoader) {
        return new ArrayList<>(jsonLoader.loadList(format(LOGS_FILE_NAME_PATTERN, id),
                Log.class));
    }

    private AuctionData getAuctionById(List<AuctionData> list, Integer id) {

        return list.stream()
                .filter(auction -> auction.getId().equals(id))
                .findAny()
                .orElse(null);

    }

    private Boolean isValidBid(Double currentBid, Double newBid) {
        return newBid > currentBid;
    }

    private BidiMap<Integer, AuctionServiceBlockingStub> buildStubsMap(Integer port) {

        BidiMap<Integer, AuctionServiceBlockingStub> stubsMap = new DualHashBidiMap<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            if (SERVER_PORT + i != port) {
                AuctionServiceBlockingStub stub = buildAuctionServerStub(buildChannel(SERVER_HOST, SERVER_PORT + i));
                stubsMap.put(SERVER_PORT + i, stub);
            }
        }

        return stubsMap;
    }

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

    private ManagedChannel buildChannel(String host, Integer port) {
        return forAddress(host, port)
                .usePlaintext()
                .build();
    }

    public static void main(String[] args) {
        AuctionServiceImpl auctionService = new AuctionServiceImpl();

//        auctionService.sendBid(SendBidRequest
//                .newBuilder()
//                .setIsServer(FALSE)
//                .setPort(50001)
//                .setId(1)
//                .setBid(19)
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

//        auctionService.getAuctions(auctionService.buildGetAuctionsRequest(50001, false),
//                new StreamObserver<GetAuctionsResponse>() {
//                    @Override
//                    public void onNext(GetAuctionsResponse getAuctionsResponse) {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//
//                    }
//
//                    @Override
//                    public void onCompleted() {
//
//                    }
//                });

        auctionService.createAuction(CreateAuctionRequest.newBuilder()
                        .setAuction(Auction.newBuilder()
                                .setOwner("tester")
                                .setCurrentBidInfo(CurrentBidInfo.newBuilder()
                                        .setValue(5.00)
                                        .setUsername("")
                                        .build())
                                .setInitialValue(5.00)
                                .setFinishTime(LocalDateTime.now().toString())
                                .setProduct("pen")
                                .build())
                        .setIsServer(FALSE)
                        .setPort(50000)
                        .build(),
                new StreamObserver<CreateAuctionResponse>() {
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
    }
}
