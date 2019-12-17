package consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.ServerConfig;
import domain.LogFunction;
import io.grpc.ManagedChannel;
import mapper.GrpcRequestAndResponseMapper;
import mapper.SaveAuctionRequestData;
import mapper.SaveBidRequestData;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import server.AuctionServiceImpl;
import server.SaveAuctionRequest;
import server.SaveBidRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.singletonList;
import static server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

public class AuctionConsumer {

    //    private static final String KAFKA_HOST = getKafkaHost();
    private static final Integer AUCTIONS_BASE_PORT = getServerPort();
    private static final String AUCTIONS_HOST = getServerHost();

//    private static final String TOPIC_NAME = "auctions-topic-%d";

    private final String bootstrapServer;
    private final String groupId;
    private final String topic;
    private final Integer serverSufix;
    private ServerConfig serverConfig;

    public AuctionConsumer(String bootstrapServer, String groupId, String topic, Integer serverSufix) {
        this.bootstrapServer = bootstrapServer;
        this.groupId = groupId;
        this.topic = topic;
        this.serverSufix = serverSufix;
    }

    public AuctionConsumer(String bootstrapServer, String groupId, String topic, Integer serverSufix, ServerConfig serverConfig) {
        this.bootstrapServer = bootstrapServer;
        this.groupId = groupId;
        this.topic = topic;
        this.serverSufix = serverSufix;
        this.serverConfig = serverConfig;
    }

    @Override
    public String toString() {
        return "AuctionConsumer{" +
                "bootstrapServer='" + bootstrapServer + '\'' +
                ", groupId='" + groupId + '\'' +
                ", topic='" + topic + '\'' +
                ", serverSufix=" + serverSufix +
                '}';
    }

    private Properties consumerProps(String bootstrapServer, String groupId) {
        String deserializer = StringDeserializer.class.getName();
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer);
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return properties;
    }

    public void run() {
        System.out.println("Creating consumer thread");

        CountDownLatch latch = new CountDownLatch(1);

        ConsumerRunnable consumerRunnable = new ConsumerRunnable(bootstrapServer, groupId, topic, latch);
        Thread thread = new Thread(consumerRunnable);
        thread.setName("consumer-node-" + getCurrentNode() + "--server-" + getCurrentServer());
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Caught shutdown hook");
            consumerRunnable.shutdown();
            await(latch);

            System.out.println("Application has exited");
        }));
    }

    private String getCurrentNode() {
        return Optional.ofNullable(serverConfig).map(s -> s.getCurrentNode().toString()).orElse(StringUtils.EMPTY);
    }

    private String getCurrentServer() {
        return Optional.ofNullable(serverConfig).map(s -> s.getCurrentServer().toString()).orElse(StringUtils.EMPTY);
    }

    public void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("Application got interrupted");
        } finally {
            System.out.println("Application is closing");
        }
    }

    private class ConsumerRunnable implements Runnable {

        private CountDownLatch mLatch;
        private KafkaConsumer<String, String> mConsumer;


        public ConsumerRunnable(String bootstrapServer, String groupId, String topic, CountDownLatch mLatch) {
            this.mLatch = mLatch;
            Properties prop = consumerProps(bootstrapServer, groupId);
            mConsumer = new KafkaConsumer<>(prop);
            mConsumer.subscribe(singletonList(topic));
        }

        @Override
        public void run() {
//            System.out.println("Running ConsumerRunnable");
            AuctionServiceImpl service = new AuctionServiceImpl();
            ObjectMapper om = new ObjectMapper();
            try {
                do {
                    ConsumerRecords<String, String> records = mConsumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        System.out.println("Consumed - Key: " + record.key() + ", Value: " + record.value());
                        LogFunction function = LogFunction.valueOf(record.key());
                        callService(service, function, record.value(), om);
                    }

                } while (true);
            } catch (WakeupException | IOException e) {
                System.out.println("Received shutdown signal!");
            } finally {
                mConsumer.close();
                mLatch.countDown();
            }
        }

        public void shutdown() {
            mConsumer.wakeup();
        }

        private void callService(AuctionServiceImpl service, LogFunction function, String value,
                                 ObjectMapper objectMapper) throws IOException {
            GrpcRequestAndResponseMapper grpcRequestAndResponseMapper = new GrpcRequestAndResponseMapper();
            ManagedChannel channel = service.buildChannel(AUCTIONS_HOST, AUCTIONS_BASE_PORT + serverSufix);
            AuctionServiceBlockingStub stub = service.buildAuctionServerStub(channel);
            System.out.println(serverSufix + "- Executing - Key: " + function + ", Value: " + value);
            switch (function) {
                case SAVE_AUCTION:
                    SaveAuctionRequestData saveAuctionRequestData = objectMapper.readValue(value,
                            SaveAuctionRequestData.class);
                    SaveAuctionRequest saveAuctionRequest =
                            grpcRequestAndResponseMapper.saveAuctionRequestFromSaveAuctionRequestData(
                                    saveAuctionRequestData);

                    stub.saveAuction(buildSaveAuctionRequestWithSufix(saveAuctionRequest));
                    break;

                case SAVE_BID:
                    SaveBidRequestData saveBidRequestData = objectMapper.readValue(value, SaveBidRequestData.class);
                    SaveBidRequest saveBidRequest =
                            grpcRequestAndResponseMapper.saveBidRequestFromSaveBidRequestData(saveBidRequestData);
                    stub.saveBid(buildSaveBidRequestWithSufix(saveBidRequest));
                    break;

                default:
                    break;
            }
        }

        private SaveAuctionRequest buildSaveAuctionRequestWithSufix(SaveAuctionRequest request) {
            return SaveAuctionRequest.newBuilder()
                    .setAuctionId(request.getAuctionId())
                    .setAuction(request.getAuction())
                    .setServerSufix(serverSufix)
                    .build();
        }

        private SaveBidRequest buildSaveBidRequestWithSufix(SaveBidRequest request) {
            return SaveBidRequest.newBuilder()
                    .setServerSufix(serverSufix)
                    .setHashTableId(request.getHashTableId())
                    .setUsername(request.getUsername())
                    .setAuctionId(request.getAuctionId())
                    .setBid(request.getBid())
                    .build();
        }

    }

    public static void main(String[] args) {

        Integer serverSufix = 0;
        String server = "localhost:9092";
//        String groupId = "some_application1";
        String topic = "auctions-topic-%s";

//        new Consumer(server, groupId, topic).run();
        AuctionConsumer auctionConsumer = new AuctionConsumer(server, UUID.randomUUID().toString(),
                String.format(topic, args[0]), Integer.valueOf(args[1]));
        auctionConsumer.run();
        System.out.println(auctionConsumer);
    }
}
