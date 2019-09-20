const net = require('net')
const DatabaseControl = require('./controllers/database_control')

const databaseControl = new DatabaseControl().getInstance();
const databaseControl2 = new DatabaseControl().getInstance();

async function singletonTest() {
    let data = await databaseControl.getFromDatabase('BETS');
    console.log(data)
    await databaseControl.setOnDatabase('BETS', {
        ['23ipo312pio']: data['23ipo312pio']
    });
    const data2 = await databaseControl2.getFromDatabase('BETS');
    data = await databaseControl.getFromDatabase('BETS');
    console.log(data)
    console.log(data2)
}


async function bootstrap(name) {
    // BETS
    let bets = await databaseControl.getFromDatabase('BETS');
    await databaseControl.setOnDatabase('BETS', bets);
    let bets2 = await databaseControl.getFromDatabase('BETS');
    console.log('BETS', JSON.stringify(bets) === JSON.stringify(bets2))

    // SESSION
    let sessions = await databaseControl.getFromDatabase('SESSION');
    await databaseControl.setOnDatabase('SESSION', sessions);
    let sessions2 = await databaseControl.getFromDatabase('SESSION');
    console.log('SESSION', JSON.stringify(sessions) === JSON.stringify(sessions2))

    // USERS
    let users = await databaseControl.getFromDatabase('USERS');
    await databaseControl.setOnDatabase('USERS', users);
    let users2 = await databaseControl.getFromDatabase('USERS');
    console.log('USERS', JSON.stringify(users) === JSON.stringify(users2))

    // PAST_AUCTIONS
    let past_auctions = await databaseControl.getFromDatabase('PAST_AUCTIONS');
    await databaseControl.setOnDatabase('PAST_AUCTIONS', past_auctions);
    let past_auctions2 = await databaseControl.getFromDatabase('PAST_AUCTIONS');
    console.log('PAST_AUCTIONS', JSON.stringify(past_auctions) === JSON.stringify(past_auctions2))
}

for (let index = 0; index < 50; index++) {
    bootstrap('test');
}
// singletonTest();