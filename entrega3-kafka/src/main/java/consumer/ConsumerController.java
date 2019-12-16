package consumer;

import static java.lang.String.format;
import static util.ConfigProperties.getKafkaHost;
import static util.ConfigProperties.getNumberOfNodes;
import static util.ConfigProperties.getNumberOfServers;
import static util.ConfigProperties.getServerHost;
import static util.ConfigProperties.getServerPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;

public class ConsumerController {

    private static final Integer NUMBER_OF_SERVERS = getNumberOfServers();

    private static final String KAFKA_HOST = getKafkaHost();
    private static final Integer NUMBER_OF_NODES = getNumberOfNodes();

    private static final String TOPIC_NAME = "auction-topic-%d";
    private static final String GROUP_ID_NAME = "auction-group-%d";

    private List<AuctionConsumer> buildAuctionConsumers() {

        List<AuctionConsumer> consumers = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
            AuctionConsumer consumer = new AuctionConsumer(
                    KAFKA_HOST,
                    format(GROUP_ID_NAME, i),
                    format(TOPIC_NAME, i % NUMBER_OF_NODES),
                    i);

            consumers.add(consumer);
        }

//        consumers.forEach(consumer -> new Thread(consumer::run));
        consumers.forEach(AuctionConsumer::run);
        return consumers;
    }

    public static void main(String[] args) {
        ConsumerController consumerController = new ConsumerController();

        System.out.println(consumerController.buildAuctionConsumers());
    }
}
