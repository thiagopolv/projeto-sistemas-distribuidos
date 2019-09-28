const DatabaseRepository = require('./DatabaseRepository');

class SessionRepository extends DatabaseRepository {

    constructor() {
        super();
        this._sessions = null;
    }

    async getSessions() {
        this._sessions = await this._getFromFile('cache/sessions', this._sessions)
        return this._sessions;
    }

    async setSessions(newValue) {
        this._sessions = await this._writeOnDatabase('cache/sessions', this._sessions, newValue);
        return this._sessions;
    }
}

class Singleton {

    constructor() {
        if (!Singleton._instance) {
            Singleton._instance = new SessionRepository();
        }
    }

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;