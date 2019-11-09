package mapper;

public class CurrentBidInfo {

    private Double value;
    private String username;

    public CurrentBidInfo() {
    }

    public CurrentBidInfo(Double value, String username) {
        this.value = value;
        this.username = username;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
