const uuidv4 = require('uuid/v4')

const AuctionRepository = require('../model/repository/AuctionRepository');
const BetRepository = require('../model/repository/BetRepository');

const auctionRepository = new AuctionRepository().getInstance();
const betRepository = new BetRepository().getInstance();

class AuctionService {

    async getAuctions() {
        const auctions = await auctionRepository.getAuctions();
        return auctions;
    }

    async createAuction(session, data) {
        const auctions = await this.getAuctions();
        const uuid = uuidv4();
        auctions[uuid] = {
            name: data.name,
            initialValue: Number(data.initialValue || 0),
            currentValue: Number(data.initialValue || 0),
            owner: session.user,
            status: "WAITING_PLAYERS",
            startedDate: new Date(Date.now() - new Date().getTimezoneOffset() * 60000),
            endDate: data.endDate,
            minutes: data.minutes,
            id: uuid
        }
        await auctionRepository.setAuctions(auctions);
        return auctions;
    }

    async verifyAuction(id, numberOfPlayers, minimumNumberOfParticipants) {
        const auctions = await this.getAuctions();
        const myAuction = auctions[id];
        if (!myAuction || myAuction.status === 'FINISHED') {
            return null;
        }
        if (myAuction.status === 'GOING_ON' && this._hasEnd(myAuction.endDate)) {
            myAuction.status = 'FINISHED';
            auctions[id] = myAuction;
            await auctionRepository.setAuctions(auctions);
            return null;
        }
        if (numberOfPlayers + 1 >= minimumNumberOfParticipants && myAuction.status === 'WAITING_PLAYERS') {
            myAuction.status = 'GOING_ON';
            myAuction.endDate = this._dateMinutesFromNow(myAuction.minutes)
            auctions[id] = myAuction;
            await auctionRepository.setAuctions(auctions);
        }
        return myAuction;
    }

    async newBid(session, data) {
        const auctions = await this.getAuctions();
        console.log(data)

        if (!auctions[data.auction.id]) {
            return { error: true, message: "Leilão não encontrado\n\n" };
        }

        if (Number(auctions[data.auction.id].currentValue) >= Number(data.bid)) {
            return { error: true, message: "\nO valor do lance é menor ou igual ao atual do Leilão\n\n" };
        }
        const bets = await betRepository.getBets();
        const newBet = this._generateBid(session.user, data.bid);
        bets[newBet.id] = newBet;

        auctions[data.auction.id].currentValue = data.bid;
        auctions[data.auction.id].lastBet = newBet.id;
        await betRepository.setBets(bets);
        await auctionRepository.setAuctions(auctions);
        return { error: false, auction: auctions[data.auction.id] }
    }

    _hasEnd(date) {
        const dateNowInCorrectZone = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
        return dateNowInCorrectZone > new Date(date);
    }

    _dateMinutesFromNow(minutes) {
        const dateNowInCorrectZone = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
        return new Date(dateNowInCorrectZone + Number(minutes || 1) * 1000 * 60)
    }

    _generateBid(user, bid) {
        return {
            id: uuidv4(),
            owner: user,
            value: bid,
            betDate: new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
        }
    }
}

module.exports = AuctionService;