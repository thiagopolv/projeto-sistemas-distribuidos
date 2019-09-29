const uuidv4 = require('uuid/v4')

const AuctionRepository = require('../model/repository/AuctionRepository');

const auctionRepository = new AuctionRepository().getInstance();

class AuctionService {

    async getAuctions() {
        const auctions = await auctionRepository.getAuctions();
        return auctions;
    }

    async createAuction(session, data) {
        const auctions = await this.getAuctions();
        const uuid = uuidv4();
        auctions[uuid] = {
            "name": data.name,
            "initialValue": data.initialValue,
            "currentValue": data.initialValue,
            "owner": session.user,
            "status": "WAITING_PLAYERS",
            "startedDate": new Date(),
            "endDate": data.endDate
        }

        return auctions;
    }
}

module.exports = AuctionService;