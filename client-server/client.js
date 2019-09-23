const net = require('net');

const nickname = 'participant1';



function createSocket() {
    const client = new net.Socket();
    return client;
}

function startConnection(client, serverPort, serverIP) {
    client.connect(serverPort, serverIP, (socket) => {
        console.log('Connected')
    })
}

function testExample() {
    let client = createSocket();
    startConnection(client, 3000, 'localhost');

    client.on('data', function (data) {
        console.log('Received: ' + data);
        client.write('Hello, server! Good bye!')
        //client.destroy(); // kill client after server's response
    })

    client.on('close', function () {
        console.log('Connection closed');
    });
}

testExample();