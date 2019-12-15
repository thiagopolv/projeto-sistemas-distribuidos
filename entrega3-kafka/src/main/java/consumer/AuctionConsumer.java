package consumer;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.singletonList;
import static server.AuctionServiceGrpc.AuctionServiceBlockingStub;
import static util.ConfigProperties.getKafkaHost;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import domain.LogFunction;
import io.grpc.ManagedChannel;
import server.AuctionServiceImpl;
import server.SaveAuctionRequest;
import server.SaveBidRequest;

public class AuctionConsumer {

    private static final String KAFKA_HOST = getKafkaHost();
    private static final Integer AUCTIONS_BASE_PORT = getServerPort();
    private static final String AUCTIONS_HOST = getServerHost();

    private static final String TOPIC_NAME ="auctions-topic-%d";

    private final String mBootstrapServer;
    private final String mGroupId;
    private final String mTopic;
    private final Integer serverSufix;

    public AuctionConsumer(String bootstrapServer, String groupId, String topic, Integer serverSufix) {
        this.mBootstrapServer = bootstrapServer;
        this.mGroupId = groupId;
        this.mTopic = topic;
        this.serverSufix = serverSufix;
    }

    private Properties consumerProps(String bootstrapServer, String groupId) {
        String deserializer = StringDeserializer.class.getName();
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, deserializer);
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }

    public void run() {
        System.out.println("Creating consumer thread");

        CountDownLatch latch = new CountDownLatch(1);

        ConsumerRunnable consumerRunnable = new ConsumerRunnable(mBootstrapServer, mGroupId, mTopic, latch);
        Thread thread = new Thread(consumerRunnable);
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Caught shutdown hook");
            consumerRunnable.shutdown();
            await(latch);

            System.out.println("Application has exited");
        }));

//        await(latch);
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
            AuctionServiceImpl service = new AuctionServiceImpl();
            ObjectMapper om = new ObjectMapper();
            try {
                do {
                    ConsumerRecords<String, String> records = mConsumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        LogFunction function = om.readValue(record.key(), LogFunction.class);
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

        private void callService(AuctionServiceImpl service, LogFunction function, String value, ObjectMapper objectMapper) throws IOException {
            ManagedChannel channel = service.buildChannel( AUCTIONS_HOST, AUCTIONS_BASE_PORT + serverSufix);
            AuctionServiceBlockingStub stub = service.buildAuctionServerStub(channel);

            switch (function) {
                case SAVE_AUCTION:
                    SaveAuctionRequest saveAuctionRequest = objectMapper.readValue(value, SaveAuctionRequest.class);
                    stub.saveAuction(buildSaveAuctionRequestWithSufix(saveAuctionRequest));

//                case SAVE_BID:
//                    SaveBidRequest saveBidRequest = objectMapper.readValue(value, SaveBidRequest.class);
//                    stub.saveBid(saveBidRequest);

                default:
                    break;
            }
        }

//        private SaveBidRequest buildSaveBidRequestWithSufix(SaveBidRequest saveBidRequest) {
//            return SaveBidRequest.newBuilder()
//                    .
//                    .build();
//        }

        private SaveAuctionRequest buildSaveAuctionRequestWithSufix(SaveAuctionRequest request) {
            return SaveAuctionRequest.newBuilder()
                    .setAuctionId(request.getAuctionId())
                    .setAuction(request.getAuction())
                    .setServerSufix(serverSufix)
                    .build();
        }

    }

    public static void main(String[] args) {

        Integer serverSufix = 0;
        String server = "localhost:9092";
//        String groupId = "some_application1";
        String topic = "user_registered";

//        new Consumer(server, groupId, topic).run();
        new AuctionConsumer(server, server, topic, serverSufix).run();
    }
}
