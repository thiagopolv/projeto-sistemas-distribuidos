const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const DATABASE_BASE_PATH = __dirname + '/../database'

class DatabaseControl {

    constructor() {
        this._bets = null;
        this._pastAuctions = null;
        this._sessions = null;
        this._users = null;
    }

    async getFromDatabase(type) {
        switch (type) {
            case 'BETS':
                return this._getFromFile('cache/bets', this._bets);
            case 'SESSION':
                return this._getFromFile('cache/sessions', this._session);
            case 'USERS':
                return this._getFromFile('relational/users', this._users);
            case 'PAST_AUCTIONS':
                return this._getFromFile('relational/past-auctions', this._pastAuctions);
        }
    }

    async setOnDatabase(type, newValue) {
        switch (type) {
            case 'BETS':
                await this._writeOnDatabase('cache/bets', this._bets, newValue);
                return;
            case 'SESSION':
                await this._writeOnDatabase('cache/sessions', this._session, newValue);
                return;
            case 'USERS':
                await this._writeOnDatabase('relational/users', this._users, newValue);
                return;
            case 'PAST_AUCTIONS':
                await this._writeOnDatabase('relational/past-auctions', this._pastAuctions, newValue);
                return;
        }
    }

    async _getFromFile(databaseSpecificPath, memoryValue) {
        if (memoryValue) {
            return memoryValue;
        }
        try {
            const read = await readFile(this._resolveDatabasePath(databaseSpecificPath))
            memoryValue = JSON.parse(read.toString());
        } catch (e) {
            console.log(e)
            this._retryOperation(memoryValue, this._writeOnDatabase);
        }

        return memoryValue;
    }

    async _writeOnDatabase(databaseSpecificPath, memoryValue, newValue) {
        if (!!newValue) {
            try {
                memoryValue = newValue;                
                await writeFile(this._resolveDatabasePath(databaseSpecificPath), JSON.stringify(memoryValue));
            }
            catch (e) {
                console.log(e)
                this._retryOperation(this._writeOnDatabase, databaseSpecificPath, memoryValue, newValue);
            }
        }
    }

    _retryOperation(callback, ...object) {
        setTimeout(function () {
            callback(...object);
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