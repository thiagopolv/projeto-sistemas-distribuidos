package producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuctionProducer {

    private final KafkaProducer<String, String> producer;

    public AuctionProducer(String boostrapServer) {
        Properties props = producerProps(boostrapServer);
        producer = new KafkaProducer<>(props);

        System.out.println("Producer initialized");
    }

    public void put(String topic, String key, String value) throws ExecutionException, InterruptedException {
        System.out.println("Put value: " + value + ", for key: " + key);

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, ((recordMetadata, e) -> {
            if (e != null) {
                System.out.println("Error while producing " + e);
                return;
            }

            System.out.println("Received new meta. \n" +
                    "Topic: " + recordMetadata.topic() + "\n" +
                    "Partition: " + recordMetadata.partition() + "\n" +
                    "Offset: " + recordMetadata.offset() + "\n" +
                    "Timestamp: " + recordMetadata.timestamp());

        })).get();
    }

    private void close() {
        System.out.println("Closing producer's connection");
        producer.close();
    }

    private Properties producerProps(String bootstrapServer) {
        String stringSerializer = StringSerializer.class.getName();
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, stringSerializer);
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, stringSerializer);

        return props;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        String server = "localhost:9092";
        String topic = "user_registered";

        AuctionProducer auctionProducer = new AuctionProducer(server);
//        producer.put(topic, "user1", "Jhon");
//        producer.put(topic, "user2", "Peter");
//        producer.put(topic, SUM.name(), BigDecimal.valueOf(10L).toString());
;
        auctionProducer.close();
    }
}

