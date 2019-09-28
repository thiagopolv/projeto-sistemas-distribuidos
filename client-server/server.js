const net = require('net');
const createServer = require('util').promisify(net.createServer);
const messages = require('./messages');

const randomPort = 3000;
const randomIP = 'localhost';
const startingBid = 10.00;
const productName = 'Carro';
const minimumNumberOfParticipants = 2;
const auctionOwner = 'RandonOwner';

let clientCount = 0;
let clients = [];
let currentBid = startingBid;
let lastBid = 0 ;
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

function socketOnDataEvent(socket, server) {
    socket.on('data', function(data) { 
        let regex = /^\d+(\.\d{1,2})?$/;
        if(regex.test(data)) {
            console.log('\nNovo Lance: R$' + data);
            multicast(messages.sentBidMessage.replace('{}', data));
            let isLessThanCurrentBid = verifyIfReceivedBidIsLessThanCurrentBid(data, socket);
            verifyIfAuctionIsFinished(data, server, isLessThanCurrentBid);
        } else {
            console.log(`Lance ${data} recebido possui formato inválido.`);
            socket.write(messages.sentBidFormatErrorMessage);
        }        
    });
}

function verifyIfReceivedBidIsLessThanCurrentBid(data, socket) {
    let dataLessThanCurrentBid = false;    
    if(data <= currentBid) {
        console.log(messages.receivedBidErrorMessage.replace('{}', data));
        socket.write(messages.sentBidErrorMessage);
        dataLessThanCurrentBid= true;
    }
    currentBid = data;
    return dataLessThanCurrentBid;
}

function verifyIfAuctionIsFinished(data, server, isLessThanCurrentBid) {
    setTimeout(() => {
        lastBid = data;
        console.log('actual: ' + currentBid);
        console.log('last:' + lastBid);
        if(currentBid == lastBid && !isLessThanCurrentBid) {
            multicast(messages.finishedAuctionMessage.replace('{}', data));
            closeSocketsAndServer(server);
        }
    }, 20000)
}

function createAuctionServer() {
    let server = net.createServer((socket) => {
        socket.setDefaultEncoding('utf-8');
        socketOnErrorEvent(socket);
        socketOnDataEvent(socket, server);
    });

    return server;
}

function socketOnErrorEvent(socket) {
    socket.on('error', function() {
        console.log(messages.clientConnectionError)
        clientCount--;
        clients.pop(socket);
    });
}

function onConnectionEvent(server) {
    server.on('connection', (socket) => {
        clientCount++;
        console.log(auctionState);
        console.log(messages.connectionMessage.replace('{0}', clientCount));
        console.log(clients.length);
        multicast(messages.connectionMessage.replace('{0}', clientCount));

        clients.push(socket);
        
        if(auctionState == 'STARTED') {
            socket.write(messages.startingMessage);
        }   

        if(auctionState == 'STARTING') {
            validateNumberOfParticipants();
        }    
    });
}

function onEndEvent(server) {
    server.on('end', (socket) => {        
        console.log(messages.endMessage);
    });
}

function onErrorEvent(server) {
    server.on('error', () => {
        console.log(messages.errorMessage);
    });
}

function multicast(message) {
    clients.forEach(function(client) {
        client.write(message);
    });
}

function closeSocketsAndServer(server) {
    clients.forEach(function(client) {
        client.destroy();
    });
    server.close();
}

function validateNumberOfParticipants() {
    if(clientCount == minimumNumberOfParticipants) {
        console.log(messages.startingMessage);
        multicast(messages.startingMessage);
        auctionState = 'STARTED';
    }    
}

function testExample() {
    const server = net.createServer((socket) => {
        socket.setDefaultEncoding('utf-8')
        socket.on('data', function (data) {
            console.log(data.toString())
        })
    });

    server.on('connection', (socket) => {
        clientCount++;
        socket.write('Hello Client! Love to be server!');
        if (clientCount > 2) {
            server.close();
        }
    });

    server.on('close', () => {
        console.log('Good bye');
    })

    // Grab an arbitrary unused port.
    server.listen('3000', 'localhost', () => {
        console.log('opened server on', server.address());
    });
}

startAuction();
//testExample();