const BetRepository = require('../model/repository/BetRepository');

const betRepository = new BetRepository().getInstance();
const betRepository2 = new BetRepository().getInstance();

async function compareGetFromDistinctFiles() {
    let bets = await betRepository.getBets();
    await betRepository.setBets(bets);
    let bets2 = await betRepository2.getBets();
    console.log('BETS', JSON.stringify(bets) === JSON.stringify(bets2))
}

compareGetFromDistinctFiles();