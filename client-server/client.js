const net = require('net');
const messages = require('./messages');
const readline = require('readline');

const nickname = 'participant1';
const randomPort = 3000;
const randomIP = 'localhost';
const input = createInterface();

let auctionState = 'STARTING';

function createSocket() {
    const client = new net.Socket();
    return client;
}

function startConnection(client, serverPort, serverIP) {
    client.connect(serverPort, serverIP, (socket) => {
        console.log('\nConectado com sucesso ao servidor.\n')
    });
}

function sendBid(socket) {    
    input.question('\nInsira o seu lance: ', (bid) => {
        socket.write(bid);
    });    
}

function connectToAuction() {
    let client = createSocket();
    startConnection(client, randomPort, randomIP);
    onDataEvent(client);
    onErrorEvent(client);
    onEndEvent(client);
    onCloseEvent(client);
}

function createInterface() {
    return readline.createInterface({
        input: process.stdin,
        output: process.stdout 
     });
}

function onDataEvent(socket) {
    socket.on('data', function(data) { 
        console.log(`\nRecebido: ${data}`);
        if(data == messages.startingMessage) {       
            auctionState = 'STARTED';
        }

        if(data == messages.sentBidErrorMessage) {
            console.log(messages.sentBidErrorMessage);
        }

        if(auctionState == 'STARTED') {
            sendBid(socket);
        }
    });
}

function onCloseEvent(socket) {
    socket.on('close', function () {
        console.log('\nConexão encerrada.');
        socket.destroy();
        process.exit(1);
    });
}

function onEndEvent(socket) {
    socket.on('end', function () {
        console.log('\nO servidor encerrou a conexão.')
        socket.destroy();
        process.exit(1);
    });
}

function onErrorEvent(socket) {
    socket.on('error', function () {
        console.log('\nErro no servidor.')
        process.exit(1);
    });
}

function testExample() {
    let client = createSocket();
    startConnection(client, 3000, 'localhost');

    client.on('data', function (data) {              
        console.log('Recebido: ' + data);
        //client.destroy(); // kill client after server's response
    })

    client.on('close', function () {
        console.log('Conexão encerrada.');
    });

    client.on('error', function () {
        console.log('Erro no servidor.')
    })

    client.on('end', function () {
        console.log('Recebido: O servidor encerrou a conexão.')
    })
}

// testExample();
connectToAuction();