const net = require('net');
const messages = require('./messages');
const actions = require('./actions');
const UserService = require('../service/UserService');
const AuctionService = require('../service/AuctionService');

const userService = new UserService();
const auctionService = new AuctionService();

const serverPort = 3000;
const serverIp = '';
let clientsSocketListWaitingAuth = [];
let clientsSocketList = [];
let auctionsSocketList = {};

const minimumNumberOfParticipants = 2;

function startAuction() {
    const server = createServer();

    server.listen(serverPort, serverIp, () => {
        console.log(`O servidor iniciou na porta ${serverPort}`);
    });

    onConnectionEvent(server);
    onEndEvent(server);
    onErrorEvent(server);
    doFinishAuctionsRoutine();
}

function socketOnDataEvent(socket) {
    socket.on('data', function (fullData) {
        processData(socket, JSON.parse(fullData));
    });
}

function doFinishAuctionsRoutine() {
    setInterval(searchAuctionsTofinish, 3000);
}

async function searchAuctionsTofinish() {
    const finishedAuctions = await auctionService.searchFinishedAuctions();
    console.log(finishedAuctions)

    if (finishedAuctions.length) {
        finishedAuctions.forEach((auction) => {
            multicast(message, {
                data: { auction: auction }, action: actions.notification.name,
                type: actions.notification.type.finishedAuction
            });
            changeSocketList(auctionsSocketList[auction.id], clientsSocketList);
            delete auctionsSocketList[auction.id];
        })
    }
}

function changeSocketList(oldSocketList, newSocketList) {
    oldSocketList.forEach((socket) => {
        addSocketIfNotOnList(socket, newSocketList);
    })
}

function processData(socket, fullData) {
    // clearConsole();
    switch (fullData.action) {
        case 'AUTH':
            processAuthenticationAction(socket, fullData);
            break;
        case 'AUCTION':
            processAuctionAction(socket, fullData);
            break;
        case 'BID':
            processBidAction(socket, fullData);
            break;
    }
}

async function processBidAction(socket, fullData) {
    const newSession = await getAuthentication(fullData.token);
    if (!newSession) {
        const obj = buildDataToSend({ message: messages.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }
    switch (fullData.type) {
        case 'NEW_BID':
            processNewBidAction(socket, fullData.data, newSession);
            break;
    }
}

async function processNewBidAction(socket, data, newSession) {
    const bidAnswer = await auctionService.newBid(newSession, data);
    auctionsSocketList[data.auction.id] = auctionsSocketList[data.auction.id] || []
    if (bidAnswer.error) {
        sendData(socket, {
            data: { auction: data.auction, message: bidAnswer.message }, token: newSession.token, action: actions.bid.name,
            type: actions.bid.type.error
        });
        return;
    }
    sendData(socket, { data: { auction: bidAnswer.auction }, token: newSession.token, action: actions.bid.name, type: actions.bid.type.success });

    addSocketIfNotOnList(socket, auctionsSocketList[data.auction.id]);

    multicast({ data: { auction: bidAnswer.auction }, action: actions.bid.name, type: actions.bid.type.update },
        auctionsSocketList[data.auction.id]);
}

function processAuthenticationAction(socket, fullData) {
    switch (fullData.type) {
        case 'LOGIN':
            processLogin(socket, fullData.data);
            break;
        case 'CREATE_USER':
            processUserCreation(socket, fullData.data);
            break;
    }
}

async function processLogin(socket, data) {
    const token = await userService.authenticate(data);
    if (token) {
        migrateToClientsList(socket);
        const obj = buildDataToSend({ message: messages.successAuthenticationMessage }, actions.auth.name, actions.auth.type.sucess, token);
        sendData(socket, obj);
        return;
    }
    const obj = buildDataToSend({ message: messages.errorAuthenticationMessage }, actions.auth.name, actions.auth.type.fail, null);
    sendData(socket, obj);
}

async function processUserCreation(socket, fullData) {
    const createdUser = await userService.createUser(fullData);
    if (createdUser) {
        migrateToClientsList(socket);
        const obj = buildDataToSend({ user: createdUser.user, message: messages.successCreatingUser },
            actions.auth.name, actions.auth.type.sucess, createdUser.token);
        sendData(socket, obj);
        return;
    }
    const obj = buildDataToSend({ message: messages.errorCreatingUser }, actions.auth.name, actions.auth.type.fail, null);
    sendData(socket, obj);
}


function migrateToClientsList(socket) {
    clientsSocketListWaitingAuth.splice(clientsSocketListWaitingAuth.indexOf(socket), 1);
    clientsSocketList.push(socket);
}

function buildDataToSend(content, action, type, token) {
    return {
        data: content, action, type, token
    }
}

function sendData(socket, data) {
    const objectStringfied = JSON.stringify(data).replace('\n', '').replace('\t', '');
    socket.write(objectStringfied);
}

async function processAuctionAction(socket, fullData) {
    const newSession = await getAuthentication(fullData.token);
    if (!newSession) {
        const obj = buildDataToSend({ message: messages.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }
    addSocketIfNotOnList(socket, clientsSocketList);

    switch (fullData.type) {
        case 'GET_AUCTION':
            getAuctions(socket, newSession);
            break;
        case 'CREATE_AUCTION':
            createAuction(socket, newSession, fullData);
            break;
        case 'AUCTION_ASSOCIATE':
            associateSocketOnAuction(newSession, fullData, socket);
            break;
    }
}

async function getAuthentication(token) {
    return await userService.verifySession(token);
}

function addSocketIfNotOnList(socket, socketList) {
    if (!socketIsOnList(socket, socketList)) {
        socketList.push(socket);
    }
}

function socketIsOnList(socket, socketList) {
    return socketList.some(storagedSocket => JSON.stringify(storagedSocket) === JSON.stringify(socket));
}

async function getAuctions(socket, newSession) {
    auctions = await auctionService.getAuctions();
    sendData(socket, { data: auctions, token: newSession.token, action: actions.auction.name, type: actions.auction.type.get });
}

async function createAuction(socket, newSession, fullData) {
    auctions = await auctionService.createAuction(newSession, fullData.data);
    sendData(socket, { token: newSession.token, action: actions.auction.name, type: actions.auction.type.successCreate });
    multicast({ data: auctions, action: actions.auction.name, type: actions.auction.type.new }, clientsSocketList);
}

async function associateSocketOnAuction(newSession, fullData, socket) {
    auctionsSocketList[fullData.data.id] = auctionsSocketList[fullData.data.id] || []

    const auctionVerify = await auctionService.verifyAuction(fullData.data.id);
    if (auctionVerify.error) {
        delete auctionsSocketList[fullData.data.id]
        sendData(socket, {
            data: fullData.data, token: newSession.token, message: auctionVerify.message, action: actions.auction.name,
            type: actions.auction.type.associateError
        });
        return;
    }

    addSocketIfNotOnList(socket, auctionsSocketList[fullData.data.id]);
    clientsSocketList = removeSocketIfIsInList(clientsSocketList, socket);

    const refreshedAuction = await auctionService.getCurrentAuctionStatus(fullData.data.id, auctionsSocketList[fullData.data.id].length,
        minimumNumberOfParticipants);

    sendData(socket, {
        data: { auction: refreshedAuction }, token: newSession.token, action: actions.auction.name,
        type: actions.auction.type.associateSuccess
    });

    multicast({
        data: { user: newSession.user, auction: refreshedAuction }, action: actions.notification.name,
        type: actions.notification.type.newUser
    }, auctionsSocketList[fullData.data.id]);
}

function clearConsole() {
    console.log('\u001B[2J\u001B[0;0f');
}

function createServer() {
    const server = net.createServer((socket) => {
        socket.setDefaultEncoding('utf-8');
        socketOnErrorEvent(socket);
        socketOnDataEvent(socket);
    });

    return server;
}

function socketOnErrorEvent(socket) {
    socket.on('error', function (err) {
        console.log(err)

        clientsSocketListWaitingAuth = removeSocketIfIsInList(clientsSocketListWaitingAuth, socket);
        clientsSocketList = removeSocketIfIsInList(clientsSocketList, socket);
        Object.keys(auctionsSocketList).forEach((auctionHash) => {
            auctionsSocketList[auctionHash] = removeSocketIfIsInList(auctionsSocketList[auctionHash], socket);
        })

    });
}

function removeSocketIfIsInList(socketList, socket) {
    return socketList.filter((storagedSocket) => {
        return JSON.stringify(storagedSocket) !== JSON.stringify(socket)
    })
}

function onConnectionEvent(server, ) {
    server.on('connection', (socket) => {
        clientsSocketListWaitingAuth.push(socket);
    });
}

function onEndEvent(server) {
    server.on('end', (socket) => {
        console.log('END')
        console.log(messages.endMessage);
    });
}

function onErrorEvent(server) {
    server.on('error', (err) => {
        console.log(err);
        console.log(messages.errorMessage);
    });
}

function multicast(message, socketList) {
    socketList.forEach(function (client) {
        client.write(JSON.stringify(message));
    });
}

startAuction();