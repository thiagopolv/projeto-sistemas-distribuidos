import static java.time.LocalDateTime.now;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import config.ServerConfig;
import io.grpc.stub.StreamObserver;
import server.Auction;
import server.AuctionServiceImpl;
import server.CreateAuctionRequest;
import server.CreateAuctionResponse;
import server.CurrentBidInfo;
import server.SendBidRequest;
import server.SendBidResponse;

public class AuctionsTest {

    private void createAuction(AuctionServiceImpl auctionService, Integer iterator) {
        auctionService.createAuction(CreateAuctionRequest.newBuilder()
                .setAuction(Auction.newBuilder()
                        .setId(sha1Hex(iterator.toString().getBytes(StandardCharsets.UTF_8)))
                        .setOwner("me" + iterator)
                        .setProduct("arroz" + iterator)
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
    }

    private void sendBid(AuctionServiceImpl auctionService, Integer iterator) {
                auctionService.sendBid(SendBidRequest.newBuilder()
                .setId(sha1Hex(iterator.toString().getBytes(StandardCharsets.UTF_8)))
                .setBid(10.0)
                .setUsername("eu" + iterator)
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
    }

    public static void main(String[] args) {

        AuctionsTest auctionsTest = new AuctionsTest();

        Map<String, String> map = new HashMap<>();
        map.put("0", "0");
        map.put("1", "5555");
        map.put("2", "aaaa");

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                ServerConfig config = new ServerConfig(0, 20000, 500, 3, map, 0);
                config.setCurrentServer(0);
                config.setCurrentServerPort(20000 + 500 * i + j);
                AuctionServiceImpl auctionService = new AuctionServiceImpl(config);

                auctionsTest.createAuction(auctionService, j + i*3);
                auctionsTest.sendBid(auctionService, j + i*3);
            }
        }



    }
}
