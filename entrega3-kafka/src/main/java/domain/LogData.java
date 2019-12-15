package domain;

public class LogData {

    private SaveAuctionLog saveAuctionData;
    private SaveBidLog saveBidData;

    public LogData() {
    }

    public LogData(SaveAuctionLog saveAuctionData, SaveBidLog saveBidData) {
        this.saveAuctionData = saveAuctionData;
        this.saveBidData = saveBidData;
    }

    public LogData(SaveAuctionLog saveAuctionData) {
        this.saveAuctionData = saveAuctionData;
    }

    public LogData(SaveBidLog saveBidData) {
        this.saveBidData = saveBidData;
    }

    public SaveAuctionLog getSaveAuctionData() {
        return saveAuctionData;
    }

    public void setSaveAuctionData(SaveAuctionLog saveAuctionData) {
        this.saveAuctionData = saveAuctionData;
    }

    public SaveBidLog getSaveBidData() {
        return saveBidData;
    }

    public void setSaveBidData(SaveBidLog saveBidData) {
        this.saveBidData = saveBidData;
    }
}
