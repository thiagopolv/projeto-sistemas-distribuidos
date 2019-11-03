package mapper;


import static java.util.Objects.isNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import domain.AuctionStatus;
import server.Auction;
import server.CurrentBidInfo;

public class AuctionMapper {

    private static final String FINISHED = AuctionStatus.FINISHED.name();
    private static final String GOING_ON = AuctionStatus.GOING_ON.name();

    public Auction auctionFromAuctionData(AuctionData auctionData) {

        LocalDateTime now = LocalDateTime.now();

        return Auction.newBuilder()
                .setId(auctionData.getId())
                .setProduct(auctionData.getProduct())
                .setInitialValue(auctionData.getInitialValue())
                .setCurrentBidInfo(buildCurrentBidInfo(getCurrentBidValue(auctionData),
                        getCurrentBidUsername(auctionData)))
                .setOwner(auctionData.getOwner())
                .setFinishTime(auctionData.getFinishTime())
                .setStatus(LocalDateTime.parse(auctionData.getFinishTime()).isAfter(now) ? GOING_ON  : FINISHED)
                .build();
    }

    public AuctionData auctionDataFromAuction(Auction auction) {

        LocalDateTime now = LocalDateTime.now();
        AuctionData auctionData = new AuctionData();

        auctionData.setId(auction.getId());
        auctionData.setCurrentBidInfo(buildCurrentBidInfo(auction));
        auctionData.setFinishTime(auction.getFinishTime());
        auctionData.setInitialValue(auction.getInitialValue());
        auctionData.setOwner(auction.getOwner());
        auctionData.setStatus(null);
        auctionData.setProduct(auction.getProduct());

        return auctionData;
    }

    private mapper.CurrentBidInfo buildCurrentBidInfo(Auction auction) {
        return new mapper.CurrentBidInfo(auction.getCurrentBidInfo().getValue(),
                auction.getCurrentBidInfo().getUsername());
    }

    public List<Auction> auctionListFromAuctionDataList(List<AuctionData> auctionDataList) {

        List<Auction> list = new ArrayList<>();

        auctionDataList.forEach(data -> {
            Auction newData = auctionFromAuctionData(data);
            list.add(newData);
        });

        return list;
    }

    private String getCurrentBidUsername(AuctionData auctionData) {
        return auctionData.getCurrentBidInfo().getUsername();
    }

    private Double getCurrentBidValue(AuctionData auctionData) {
        return isNull(auctionData.getCurrentBidInfo().getValue()) ? 0.00 : auctionData.getCurrentBidInfo().getValue();
    }

    private CurrentBidInfo buildCurrentBidInfo(Double value, String username ) {
        return CurrentBidInfo.newBuilder()
                .setValue(value)
                .setUsername(username)
                .build();
    }
}
