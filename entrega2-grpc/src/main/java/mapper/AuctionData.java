package mapper;

import java.util.Objects;

import domain.AuctionStatus;

public class AuctionData {

    private String id;
    private String owner;
    private String product;
    private Double initialValue;
    private CurrentBidInfo currentBidInfo;
    private String finishTime;
    private AuctionStatus status;

    public AuctionData() {
    }

    public AuctionData(String id, String owner, String product, Double initialValue, CurrentBidInfo currentBidInfo,
            String finishTime, AuctionStatus status) {
        this.id = id;
        this.owner = owner;
        this.product = product;
        this.initialValue = initialValue;
        this.currentBidInfo = currentBidInfo;
        this.finishTime = finishTime;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public CurrentBidInfo getCurrentBidInfo() {
        return currentBidInfo;
    }

    public void setCurrentBidInfo(CurrentBidInfo currentBidInfo) {
        this.currentBidInfo = currentBidInfo;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionData that = (AuctionData) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuctionData{" +
                "id=" + id +
                ", owner='" + owner + '\'' +
                ", product='" + product + '\'' +
                ", initialValue=" + initialValue +
                ", currentBidInfo=" + currentBidInfo +
                ", finishTime='" + finishTime + '\'' +
                ", status=" + status +
                '}';
    }
}
