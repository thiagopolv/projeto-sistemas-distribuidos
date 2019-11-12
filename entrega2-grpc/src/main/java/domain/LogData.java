package domain;

public class LogData {

    private CreateAuctionLog createAuctionData;
    private SendBidLog sendBidData;

    public LogData() {
    }

    public LogData(CreateAuctionLog createAuctionData, SendBidLog sendBidData) {
        this.createAuctionData = createAuctionData;
        this.sendBidData = sendBidData;
    }

    public LogData(CreateAuctionLog createAuctionData) {
        this.createAuctionData = createAuctionData;
    }

    public LogData(SendBidLog sendBidData) {
        this.sendBidData = sendBidData;
    }

    public CreateAuctionLog getCreateAuctionData() {
        return createAuctionData;
    }

    public void setCreateAuctionData(CreateAuctionLog createAuctionData) {
        this.createAuctionData = createAuctionData;
    }

    public SendBidLog getSendBidData() {
        return sendBidData;
    }

    public void setSendBidData(SendBidLog sendBidData) {
        this.sendBidData = sendBidData;
    }
}
