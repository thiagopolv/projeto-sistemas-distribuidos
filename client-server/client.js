const net = require('net');
const messages = require('messages');

const nickname = 'participant1';

function createSocket() {
    const client = new net.Socket();
    return client;
}

function startConnection(client, serverPort, serverIP) {
    client.connect(serverPort, serverIP, (socket) => {
        console.log('Conectado com sucesso ao servidor.\n')
    })
}

function onDataEvent(socket) {
    socket.on('data', function(data) {
        if(data == messages.startingMessage) {
            startBids();
        }
    });
}

function startBids() {
    
}

function testExample() {
    let client = createSocket();
    startConnection(client, 3000, 'localhost');

    client.on('data', function (data) {
        console.log('Recebido: ' + data);
        client.write('Hello, server! Good bye!')
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

testExample();