const net = require('net');

const client = new net.Socket();

client.connect(3000, 'localhost', (socket) => {
    console.log('Connected')
})

client.on('data', function (data) {
    console.log('Received: ' + data);
    client.write('Hello, server! Good bye!')
    client.destroy(); // kill client after server's response
})

client.on('close', function () {
    console.log('Connection closed');
});