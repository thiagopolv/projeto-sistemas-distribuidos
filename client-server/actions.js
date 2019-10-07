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
            successCreate: "SUCCESS_CREATE_AUCTION",
            associate: 'AUCTION_ASSOCIATE',
            associateSuccess: 'AUCTION_ASSOCIATE_SUCCESS',
            associateError: 'AUCTION_ASSOCIATE_ERROR',
            new: "NEW_AUCTION"
        }
    },
    notification: {
        name: 'NOTIFICATION',
        type: {
            newUser: 'NEW_USER_JOINNED',
            finishedAuction: 'FINISHED_AUCTION'
        },
    },
    bid: {
        name: 'BID',
        type: {
            new: 'NEW_BID',
            update: 'UPDATED_BID',
            error: 'ERROR_BID',
            success: "SUCESS_NEW_BID"
        }
    }
};