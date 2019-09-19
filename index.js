const fs = require('fs');
const readFile = require('util').promisify(fs.readFile);
const writeFile = require('util').promisify(fs.writeFile);
const net = require('net')


async function bootstrap(name) {
    const data = await readFile(__dirname + '/database/users.json');
    const realData = JSON.parse(data.toString());
    if (realData[name]) {
        console.log(realData[name])
    } else {
        console.log("Manjou uma rola")
    }
}

bootstrap('thiago');
