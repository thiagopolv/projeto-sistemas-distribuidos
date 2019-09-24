const net = require('net');
const createServer = require('util').promisify(net.createServer);

let clientCount = 0;
let clients = [];

const randomPort = 3000;
randomIP = 'localhost';
const startingBid = 10.00;
const productName = 'Carro';
const minimumNumberOfParticipants = 3;
const auctionOwner = 'RandonOwner';

function startAuction() {
    const server = createAuctionServer();

    server.listen(randomPort, randomIP, () => {
        console.log(`Iniciando o leilão do ${productName} de ${auctionOwner}.`);
        console.log(`São necessários ${minimumNumberOfParticipants} participantes para começar.`);
        console.log(`O lance inicial é de R$${startingBid}`);
    });

    onConnectionEvent(server);
}

function createAuctionServer() {
    return net.createServer((socket) => {
        socket.setDefaultEncoding('utf-8')        
    });
}

function onConnectionEvent(server) {
    server.on('connection', (socket) => {
        //validateNumberOfParticipantsStart(socket);
        clients.push(socket);
        console.log('Um novo participante se conectou ao leilão.');
        broadcast('Um novo participante se conectou ao leilão.');
        clientCount++;
        validateNumberOfParticipantsFinal(server);
    });monitor
}

function broadcast(message) {
    clients.forEach(function(client) {
        client.write(message);
    })      
}

function validateNumberOfParticipantsStart(socket) {
    if(clientCount >= minimumNumberOfParticipants) {
        console.log('Não há mais vagas disponíveis nesse leilão.');
        socket.destroy();
    }
}

function validateNumberOfParticipantsFinal(server) {
    if(clientCount == minimumNumberOfParticipants) {
        console.log('Número mínimo de participantes atingido. Iniciando leilão...');
    }
    broadcast('Número mínimo de participantes atingido. Iniciando leilão...');
    server.close();
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