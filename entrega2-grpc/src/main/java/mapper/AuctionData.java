package mapper;

import static domain.AuctionStatus.GOING_ON;

import java.time.LocalDateTime;

import domain.AuctionStatus;

public class AuctionData {

    private Integer id;
    private String owner;
    private Double initialValue;
    private Double currentBid;
    private String finishTime;
    private AuctionStatus status;

    public AuctionData() {
    }

    public AuctionData(Integer id, String owner, Double initialValue, Double currentBid, String finishTime) {
        this.id = id;
        this.owner = owner;
        this.initialValue = initialValue;
        this.currentBid = currentBid;
        this.finishTime = finishTime;
        this.status = GOING_ON;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Double getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Double initialValue) {
        this.initialValue = initialValue;
    }

    public Double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(Double currentBid) {
        this.currentBid = currentBid;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public static AuctionData build(Integer id, String owner, Double initialValue, String finishTime) {
        return new AuctionData(id, owner, initialValue, null, finishTime);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
                ", owner='" + owner + '\'' +
                ", initialValue=" + initialValue +
                ", currentBid=" + currentBid +
                ", finishTime=" + finishTime +
                ", status=" + status +
                '}';
    }
}
