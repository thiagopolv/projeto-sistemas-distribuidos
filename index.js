const fs = require('fs');
const readFile = require('util').promisify(fs.readFile);
const writeFile = require('util').promisify(fs.writeFile);
const net = require('net')
const DatabaseControl = require('./database_control')

const databaseControl = new DatabaseControl().getInstance();

async function bootstrap() {
    console.log(await databaseControl.getBets())
}

