const net = require('net');

const server = net.createServer((socket) => {
    socket.setDefaultEncoding('utf-8')
    socket.write('Hello Client! Love to be server!');
    socket.on('data', function (data) {
        console.log(data.toString())
    })
});

server.on('connection', (socket) => {
    socket.write('Hello Client! Love to be server!');
});

// Grab an arbitrary unused port.
server.listen('3000', 'localhost', () => {
    console.log('opened server on', server.address());
});

// server.close();