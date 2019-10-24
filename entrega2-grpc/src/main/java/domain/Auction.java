package domain;

public class Auction {

    private Integer id;
    private String owner;
    private Double initialValue;
    private Double currentBid;

    public Auction() {
    }

    public Auction(Integer id, String owner, Double initialValue, Double currentBid) {
        this.id = id;
        this.owner = owner;
        this.initialValue = initialValue;
        this.currentBid = currentBid;
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

    public static Auction build(Integer id, String owner, Double initialValue) {
        return new Auction(id, owner, initialValue, null);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
                ", owner='" + owner + '\'' +
                ", initialValue=" + initialValue +
                ", currentBid=" + currentBid +
                '}';
    }
}
