const DatabaseRepository = require('./DatabaseRepository');

class BetRepository extends DatabaseRepository {

    constructor() {
        super();
        this._bets = null;
    }

    async getBets() {
        this._bets = await this._getFromFile('relational/bets', this._bets)
        return this._bets;
    }

    async setBets(newValue) {
        this._bets = await this._writeOnDatabase('relational/bets', this._bets, newValue);
        return this._bets;
    }
}

class Singleton {

    constructor() {
        if (!Singleton._instance) {
            Singleton._instance = new BetRepository();
        }
    }

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;