const net = require('net');
const messages = require('./messages');
const actions = require('./actions');
const UserService = require('../service/UserService');
const AuctionService = require('../service/AuctionService');

const userService = new UserService();
const auctionService = new AuctionService();

const { serverPort, serverIpAnswering: serverIp, minimumNumberOfParticipants } = require('../config')
let clientsSocketListWaitingAuth = [];
let clientsSocketList = [];
let auctionsSocketList = {};

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

    if (finishedAuctions.length) {
        finishedAuctions.forEach((auction) => {
            multicast({
                data: { auction: auction }, action: actions.notification.name,
                type: actions.notification.type.finishedAuction
            }, auctionsSocketList[auction.id]);
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

async function processData(socket, fullData) {
    // clearConsole();
    switch (fullData.action) {
        case 'AUTH':
            await processAuthenticationAction(socket, fullData);
            break;
        case 'AUCTION':
            await processAuctionAction(socket, fullData);
            break;
        case 'BID':
            await processBidAction(socket, fullData);
            break;
    }
}

async function processBidAction(socket, fullData) {
    const newSession = await getAuthentication(fullData.token);
    if (!newSession) {
        const obj = buildDataToSend({ message: messages.server.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }
    switch (fullData.type) {
        case 'NEW_BID':
            await processNewBidAction(socket, fullData.data, newSession);
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

async function processAuthenticationAction(socket, fullData) {
    switch (fullData.type) {
        case 'LOGIN':
            await processLogin(socket, fullData.data);
            break;
        case 'CREATE_USER':
            await processUserCreation(socket, fullData.data);
            break;
    }
}

async function processLogin(socket, data) {
    const token = await userService.authenticate(data);
    if (token) {
        migrateToClientsList(socket);
        const obj = buildDataToSend({ message: messages.server.successAuthenticationMessage }, actions.auth.name, actions.auth.type.sucess, token);
        sendData(socket, obj);
        return;
    }
    const obj = buildDataToSend({ message: messages.server.errorAuthenticationMessage }, actions.auth.name, actions.auth.type.fail, null);
    sendData(socket, obj);
}

async function processUserCreation(socket, fullData) {
    const createdUser = await userService.createUser(fullData);
    if (createdUser) {
        migrateToClientsList(socket);
        const obj = buildDataToSend({ user: createdUser.user, message: messages.server.successCreatingUser },
            actions.auth.name, actions.auth.type.sucess, createdUser.token);
        sendData(socket, obj);
        return;
    }
    const obj = buildDataToSend({ message: messages.server.errorCreatingUser }, actions.auth.name, actions.auth.type.fail, null);
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
        const obj = buildDataToSend({ message: messages.server.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }
    clientsSocketListWaitingAuth = removeSocketIfIsInList(clientsSocketListWaitingAuth, socket);

    switch (fullData.type) {
        case 'GET_AUCTION':
            await getAuctions(socket, newSession);
            break;
        case 'CREATE_AUCTION':
            await createAuction(socket, newSession, fullData);
            break;
        case 'AUCTION_ASSOCIATE':
            await associateSocketOnAuction(newSession, fullData.data, socket);
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
    addSocketIfNotOnList(socket, clientsSocketList);
    sendData(socket, { data: auctions, token: newSession.token, action: actions.auction.name, type: actions.auction.type.get });
}

async function createAuction(socket, newSession, fullData) {
    addSocketIfNotOnList(socket, clientsSocketList);
    auctions = await auctionService.createAuction(newSession, fullData.data);
    if (auctions.error) {
        sendData(socket, { message: auctions.message, token: newSession.token, action: actions.auction.name, type: actions.auction.type.errorCreate });
        return;
    }
    sendData(socket, { token: newSession.token, action: actions.auction.name, type: actions.auction.type.successCreate });
    multicast({ data: auctions, action: actions.auction.name, type: actions.auction.type.new }, clientsSocketList);
}

async function associateSocketOnAuction(newSession, data, socket) {

    const auctionVerify = await auctionService.verifyAuction(data);
    if (auctionVerify.error) {
        if (data) delete auctionsSocketList[data.id]
        sendData(socket, {
            data: data, token: newSession.token, message: auctionVerify.message, action: actions.auction.name,
            type: actions.auction.type.associateError
        });
        return;
    }
    removeSocketIfNecessary(socket, data);

    const refreshedAuction = await auctionService.getCurrentAuctionStatus(data.id, auctionsSocketList[data.id].length,
        minimumNumberOfParticipants);

    sendMessagesIfSucessfully(socket, newSession, refreshedAuction, data);
}

function sendMessagesIfSucessfully(socket, newSession, refreshedAuction, { id }) {
    multicast({
        data: { user: newSession.user, auction: refreshedAuction }, action: actions.notification.name,
        type: actions.notification.type.newUser
    }, auctionsSocketList[id]);
    
    sendData(socket, {
        data: { auction: refreshedAuction }, token: newSession.token, action: actions.auction.name,
        type: actions.auction.type.associateSuccess
    });
}

function removeSocketIfNecessary(socket, { id }) {
    auctionsSocketList[id] = auctionsSocketList[id] || []
    auctionsSocketList[id].push(socket)
    clientsSocketList = removeSocketIfIsInList(clientsSocketList, socket);
    clientsSocketListWaitingAuth = removeSocketIfIsInList(clientsSocketListWaitingAuth, socket);
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
        console.log(messages.server.endMessage);
    });
}

function onErrorEvent(server) {
    server.on('error', (err) => {
        console.log(err);
        console.log(messages.server.errorMessage);
    });
}

function multicast(message, socketList) {
    socketList.forEach(function (client) {
        client.write(JSON.stringify(message));
    });
}

startAuction();