const net = require('net');
const createServer = require('util').promisify(net.createServer);
const messages = require('messages');

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

connectionMessage.replace

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

function startBids() {
    
}

function onDataEvent() {

}

function createAuctionServer() {
    return server = net.createServer((socket) => {
        socket.setDefaultEncoding('utf-8');
        socketOnErrorEvent(socket);
    });    
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
        clients.push(socket);
        clientCount++;
        console.log(messages.connectionMessage.replace('{0}', clientCount));
        broadcast(messages.connectionMessage.replace('{0}', clientCount));        
        validateNumberOfParticipantsFinal();
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

function broadcast(message) {
    clients.forEach(function(client) {
        client.write(message);
    })      
}

function validateNumberOfParticipantsFinal() {
    console.log(`clientCount: ${clientCount}\n`)
    if(clientCount == minimumNumberOfParticipants) {
        console.log(messages.startingMessage);
        broadcast(messages.startingMessage);
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