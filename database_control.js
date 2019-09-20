const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const DATABASE_BASE_PATH = __dirname + '/database'

class DatabaseControl {

    constructor() {
        this._bets = null;
        this._past_auctions = null;
        this._session = null;
        this._users = null;
    }

    async getBets() {

        if (this._bets) {
            console.log('get default')
            return this._bets;
        }

        let read = await readFile(this._resolveDatabasePath('cache/bets'))
        this._bets = JSON.parse(read.toString());
        return this._bets;
    }

    async setBets(bets) {
        this._bets = bets;

        if (!!bets) {
            try {
                await writeFile(this._resolveDatabasePath('cache/bets'), JSON.stringify(bets));
            }
            catch (e) {
                console.log(e)
                this._retryOperation(bets, this.setBets);
            }
        }
    }

    _retryOperation(object, callback) {
        setTimeout(function () {
            callback(object);
        }, 2000);
    }

    _resolveDatabasePath(db) {
        return `${DATABASE_BASE_PATH}/${db}.json`
    }
}

class Singleton {

    constructor() {
        if (!Singleton._instance) {
            Singleton._instance = new DatabaseControl();
        }
    }

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;