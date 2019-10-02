const uuidV4 = require('uuid/v4');
const generateToken = require('../util/token')
const cryptoJs = require('crypto-js');

const UserRepository = require('../model/repository/UserRepository');
const SessionRepository = require('../model/repository/SessionRepository');

const userRepository = new UserRepository().getInstance();
const sessionRepository = new SessionRepository().getInstance();

class UserService {

    async authenticate(data) {
        try {
            const users = await userRepository.getUsers();
            const singleUser = users[data.user];
            if (singleUser && singleUser.password === cryptoJs.SHA256(data.password).toString(cryptoJs.enc.Hex)) {
                const session = await this.getNewSession(this._factorySessionObject(singleUser.user));
                return session.token;
            } else {
                return null;
            }
        } catch (ex) {
            console.log(ex)
        }
    }

    async verifySession(oldToken) {
        const sessions = await sessionRepository.getSessions()
        const singleSession = sessions[oldToken];

        if (singleSession && this._validadeSessionExpiration(singleSession.date)) {
            delete sessions[oldToken];
            const newSession = await this._renovateSession(singleSession.user);
            return { token: newSession.token, user: newSession.user }
        }
        if (singleSession) {
            delete sessions[oldToken];
            await sessionRepository.setSessions(sessions);
        }
        return null;
    }

    _validadeSessionExpiration(date) {
        const dateNowInCorrectZone = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
        const lastAceptableDate = dateNowInCorrectZone - 1000 * 60 * 20; // 20 minutes
        return new Date(date) - new Date(lastAceptableDate) > 0;
    }

    async _renovateSession(user) {
        return await this.getNewSession(this._factorySessionObject(user));
    }

    async createUser(data) {
        try {
            const users = await userRepository.getUsers();
            const singleUser = users[data.user];
            if (!singleUser) {
                users[data.user] = {
                    user: data.user.trim(),
                    name: '',
                    password: cryptoJs.SHA256(data.password).toString(cryptoJs.enc.Hex)
                };
                await userRepository.setUsers(users);
                const session = await this.getNewSession(this._factorySessionObject(data.user), data.user);
                return {
                    user: users[data.user],
                    token: session.token
                };
            }
            return null;
        } catch (ex) {
            console.log(ex)
        }
    }

    async getNewSession(newSession) {
        const sessions = await sessionRepository.getSessions();
        const oldSessions = Object.values(sessions).filter(element => element.user === newSession.user)
        oldSessions.forEach((obj) => {
            delete sessions[obj.token]
        })
        sessions[newSession.token] = {
            user: newSession.user, date: newSession.date, token: newSession.token
        };
        await sessionRepository.setSessions(sessions);
        return newSession;
    }

    _factorySessionObject(username) {
        return {
            token: generateToken(32),
            date: new Date(Date.now() - new Date().getTimezoneOffset() * 60000),
            user: username
        }
    }
}

module.exports = UserService;