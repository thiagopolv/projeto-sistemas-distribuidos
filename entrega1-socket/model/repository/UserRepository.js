const DatabaseRepository = require('./DatabaseRepository');

class UserRepository extends DatabaseRepository {

    constructor() {
        super();
        this._users = null;
    }

    async getUsers() {
        this._users = await this._getFromFile('relational/users', this._users)
        return this._users;
    }

    async setUsers(newValue) {
        this._users = await this._writeOnDatabase('relational/users', this._users, newValue);
        return this._users;
    }
}

class Singleton {

    constructor() {
        if (!Singleton._instance) {
            Singleton._instance = new UserRepository();
        }
    }

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;