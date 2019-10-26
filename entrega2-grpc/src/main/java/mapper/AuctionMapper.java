package mapper;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import domain.AuctionStatus;
import server.Auction;

public class AuctionMapper {

    private static final String FINISHED = AuctionStatus.FINISHED.name();
    private static final String GOING_ON = AuctionStatus.GOING_ON.name();

    private Auction auctionFromAuctionData(AuctionData auctionData) {

        LocalDateTime now = LocalDateTime.now();

        return Auction.newBuilder()
                .setId(auctionData.getId())
                .setInitialValue(auctionData.getInitialValue())
                .setCurrentBid(auctionData.getCurrentBid() != null ? auctionData.getCurrentBid() : 0)
                .setOwner(auctionData.getOwner())
                .setFinishTime(auctionData.getFinishTime())
                .setStatus(LocalDateTime.parse(auctionData.getFinishTime()).isAfter(now) ? GOING_ON  : FINISHED)
                .build();
    }

    public List<Auction> auctionListFromAuctionDataList(List<AuctionData> auctionDataList) {

        List<Auction> list = new ArrayList<>();

        auctionDataList.forEach(data -> {
            Auction newData = auctionFromAuctionData(data);
            list.add(newData);
        });

        return list;
    }
}
