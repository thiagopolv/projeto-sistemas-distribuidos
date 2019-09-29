const net = require('net');
const messages = require('./messages');
const actions = require('./actions');
const UserService = require('../service/UserService');
const AuctionService = require('../service/AuctionService');

const userService = new UserService();
const auctionService = new AuctionService();
const randomPort = 3000;
const randomIP = 'localhost';
const startingBid = 10.00;
const productName = 'Carro';
const minimumNumberOfParticipants = 2;
const auctionOwner = 'RandonOwner';

let clientWaitingAuthCount = 0;
let clientsSocketListWaitingAuth = [];

let clientCount = 0;
let clientsSocketList = [];
let currentBid = startingBid;
let lastBid = 0;
let auctionState = 'STARTING';

function startAuction() {
    const server = createAuctionServer();

    server.listen(randomPort, randomIP, () => {
        console.log(`Iniciando o leilão do ${productName} de ${auctionOwner}.`);
        console.log(`São necessários ${minimumNumberOfParticipants} participantes para começar.`);
        console.log(`O lance inicial é de R$${startingBid} \n`);
    });

    onConnectionEvent(server);
    onEndEvent(server);
    onErrorEvent(server);
}

function socketOnDataEvent(socket) {
    socket.on('data', function (data) {
        processData(socket, JSON.parse(data));
    });
}

function processData(socket, data) {
    // clearConsole();
    switch (data.action) {
        case 'AUTH':
            processAuthenticationAction(socket, data);
            break;
        case 'AUCTION':
            processAuctionAction(socket, data);
            break;
        case 'BID':
            break;
    }
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
    clientWaitingAuthCount--;
    clientsSocketListWaitingAuth.splice(clientsSocketListWaitingAuth.indexOf(socket), 1);
    clientsSocketList.push(socket);
    clientCount++;
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

function processAuctionAction(socket, fullData) {
    const newSession = getAuthentication(fullData.token);
    if (!newSession) {
        const obj = buildDataToSend({ message: messages.expireSessionMessage }, actions.auth.name, actions.auth.type.expire, null);
        sendData(socket, obj);
        return;
    }

    switch (fullData.type) {
        case 'GET_AUCTION':
            getAuctions(socket, newSession);
            break;
        case 'CREATE_AUCTION':
            createAuction(newSession, fullData);
            break;
        case 'AUCTION_ASSOCIATE':
            break;
    }
}

function getAuthentication(token) {
    return userService.verifySession(token);
}

async function getAuctions(socket, newSession) {
    console.log('cheguei em get auctions')
    auctions = await auctionService.getAuctions();
    sendData(socket, { data: auctions, token: newSession.token, action: actions.auction.name, type: actions.auction.type.get });
}

async function createAuction(newSession, fullData) {
    auctions = await auctionService.createAuction(newSession, fullData.data);
    multicast({ data: auctions, action: actions.auction.name, type: actions.auction.type.new });
}

function onClientConnected() {
    console.log(auctionState);
    console.log(messages.connectionMessage.replace('{0}', clientCount));
    console.log(clientsSocketList.length);
    multicast(messages.connectionMessage.replace('{0}', clientCount));
    clientCount++;
    clientsSocketList.push(socket);

    if (auctionState == 'STARTED') {
        socket.write(messages.startingMessage);
        return;
    }

    validateNumberOfParticipants();
}

function vaidateNewBid() {
    const regex = /^\d+(\.\d{1,2})?$/;
    if (regex.test(data)) {
        console.log('\nNovo Lance: R$' + data);
        multicast(messages.sentBidMessage.replace('{}', data));
        const isLessThanCurrentBid = verifyIfReceivedBidIsLessThanCurrentBid(data, socket);
        verifyIfAuctionIsFinished(data, server, isLessThanCurrentBid);
    } else {
        console.log(`Lance ${data} recebido possui formato inválido.`);
        socket.write(messages.sentBidFormatErrorMessage);
    }
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

function createAuctionServer() {
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
        console.log(clientsSocketList)
        
        clientsSocketListWaitingAuth = clientsSocketListWaitingAuth.filter((storagedSocket) => {
            if (JSON.stringify(storagedSocket) === JSON.stringify(socket)) {
                clientWaitingAuthCount--;
                return false;
            }
            return true;
        })
        clientsSocketList = clientsSocketList.filter((storagedSocket) => {
            if (JSON.stringify(storagedSocket) === JSON.stringify(socket)) {
                clientCount--;
                return false;
            }
            return true;
        })
        console.log(clientsSocketList)
    });
}

function onConnectionEvent(server) {
    server.on('connection', (socket) => {
        clientWaitingAuthCount++;
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

function multicast(message) {
    clientsSocketList.forEach(function (client) {
        client.write(message);
    });
}

function closeSocketsAndServer(server) {
    clientsSocketList.forEach(function (client) {
        client.destroy();
    });
    server.close();
}

function validateNumberOfParticipants() {
    if (clientCount == minimumNumberOfParticipants) {
        console.log(messages.startingMessage);
        multicast(messages.startingMessage);
        auctionState = 'STARTED';
    }
}

startAuction();