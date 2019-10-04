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
}

function socketOnDataEvent(socket) {
    socket.on('data', function (fullData) {
        processData(socket, JSON.parse(fullData));
    });
}

function processData(socket, fullData) {
    // clearConsole();
    console.log(fullData)
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
            processNewBidAction(socket, fullData, newSession);
            break;
    }
}

async function processNewBidAction(socket, fullData, newSession) {
    const bidAnswer = await auctionService.newBid(newSession, fullData.data);
    if (bidAnswer.error) {
        sendData(socket, {
            data: { auction: fullData.data.auction, message: bidAnswer.message }, token: newSession.token, action: actions.bid.name,
            type: actions.bid.type.error
        });
        return;
    }

    sendData(socket, { data: { auction: bidAnswer.auction }, token: newSession.token, action: actions.bid.name, type: actions.bid.type.success });
    multicast({ data: { auction: bidAnswer.auction }, action: actions.bid.name, type: actions.bid.type.update },
        auctionsSocketList[bidAnswer.auction.id]);
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
    const data = {
        data: content, action, type, token
    }
    return data;
}

function sendData(socket, data) {
    socket.write(JSON.stringify(data));
}

async function processAuctionAction(socket, fullData) {
    const newSession = await getAuthentication(fullData.token);
    if (!newSession) {
        const obj = buildDataToSend({ message: messages.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }
    addSocketIfNotOnList(socket);

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

function addSocketIfNotOnList(socket) {
    if (socketIsNotOnList(socket)) {
        clientsSocketList.push(socket);
    }
}

function socketIsNotOnList(socket) {
    return clientsSocketList.some(storagedSocket => JSON.stringify(storagedSocket) !== JSON.stringify(socket));
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
    console.log('\n\n')
    console.log(newSession, fullData)
    auctionsSocketList[fullData.data.id] = auctionsSocketList[fullData.data.id] || []

    const auctionVerify = await auctionService.verifyAuction(fullData.data.id, auctionsSocketList[fullData.data.id].length, minimumNumberOfParticipants);
    if (!fullData.data.id || auctionVerify.error) {
        sendData(socket, {
            data: fullData.data, token: newSession.token, message: auctionVerify.message, action: actions.auction.name,
            type: actions.auction.type.associateError
        });
        return;
    }
    console.log(auctionsSocketList[fullData.data.id])
    console.log(auctionsSocketList[fullData.data.id].length)

    auctionsSocketList[fullData.data.id].push(socket);
    removeSocketIfIsInList(clientsSocketList, socket);
    sendData(socket, { data: fullData.data, token: newSession.token, action: actions.auction.name, type: actions.auction.type.associateSuccess });
    multicast({
        data: { user: newSession.user, auction: fullData.data }, action: actions.notification.name,
        type: actions.notification.type.newUser
    }, auctionsSocketList[fullData.data.id]);
}

function clearConsole() {
    console.log('\u001B[2J\u001B[0;0f');
}

function verifyIfReceivedBidIsLessThanCurrentBid(data, socket) {
    let dataLessThanCurrentBid = false;
    if (data <= currentBid) {
        console.log(messages.receivedBidErrorMessage.replace('{}', data));
        socket.write(messages.sentBidErrorMessage);
        dataLessThanCurrentBid = true;
    }
    currentBid = data;
    return dataLessThanCurrentBid;
}

function verifyIfAuctionIsFinished(data, server, isLessThanCurrentBid) {
    setTimeout(() => {
        lastBid = data;
        console.log('actual: ' + currentBid);
        console.log('last:' + lastBid);
        if (currentBid == lastBid && !isLessThanCurrentBid) {
            multicast(messages.finishedAuctionMessage.replace('{}', data));
            // closeSocketsAndServer(server);
        }
    }, 20000)
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