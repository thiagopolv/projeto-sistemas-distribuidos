const DatabaseRepository = require('./DatabaseRepository');

class AuctionRepository extends DatabaseRepository {

    constructor() {
        super();
        this._auctions = null;
    }

    async getAuctions() {
        this._auctions = await this._getFromFile('relational/auctions', this._auctions)
        return this._auctions;
    }

    async setAuctions(newValue) {
        this._auctions = await this._writeOnDatabase('relational/auctions', this._auctions, newValue);
        return this._auctions;
    }
}

class Singleton {

    constructor() {
        if (!Singleton._instance) {
            Singleton._instance = new AuctionRepository();
        }
    }

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;