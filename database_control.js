const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

class DatabaseControl {

    constructor() {
        this._bets = null;       
        this._past_auctions = null;       
        this._session = null;       
        this._users = null;       
    }

    async getBets() {
        
        if(this._bets) {
            return this.bets;
        } 

        let read = await readFile(__dirname + '/database/bets.json')
        return JSON.parse(read.toString());        
    }

    async setBets(bets) {
        
        if(!!bets) {
            await writeFile(__dirname + '/datebase/bets.json', JSON.stringify(bets));            
        }
    }       

  
}

class Singleton {

    constructor() {        
        if(!Singleton._instance) {
            Singleton._instance = new DatabaseControl();
        } 
    } 

    getInstance() {
        return Singleton._instance;
    }
}

module.exports = Singleton;