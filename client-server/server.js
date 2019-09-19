const net = require('net');
const createServer = require('util').promisify(net.createServer);

let clientCount = 0;

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