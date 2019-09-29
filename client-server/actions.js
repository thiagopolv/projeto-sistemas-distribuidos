module.exports = actions = {
    auth: {
        name: 'AUTH',
        type: {
            login: 'LOGIN',
            createUser: 'CREATE_USER',
            sucess: 'AUTH_SUCCESS',
            fail: 'AUTH_FAIL',
            expire: 'TOKEN_EXPIRED'
        }
    },
    auction: {
        name: 'AUCTION',
        type: {
            get: 'GET_AUCTION',
            create: 'CREATE_AUCTION',
            associate: 'AUCTION_ASSOCIATE',
            new: "NEW_AUCTION"
        }
    },
    notification: {
        name: 'NOTIFICATION',
        type: {
            newUser: 'NEW_USER_JOINNED',
            newAuctionsAvailable: 'NEW_AUCTIONS_AVAILABLE',
        }
    }
};