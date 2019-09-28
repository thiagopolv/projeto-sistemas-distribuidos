const BetRepository = require('../model/repository/BetRepository');
const Auction = require('../model/repository/Auction');
const SessionRepository = require('../model/repository/SessionRepository');
const UserRepository = require('../model/repository/UserRepository');

const betRepository = new BetRepository().getInstance();
const auctionRepository = new Auction().getInstance();
const sessionRepository = new SessionRepository().getInstance();
const userRepository = new UserRepository().getInstance();

async function getAndSetFromAllDatabases() {

    // BETS
    let bets = await betRepository.getBets();
    console.log('BETS before set', bets)
    let bets2 = await betRepository.setBets(bets);
    console.log('BETS after set', bets2)

    //SESSION
    let session = await sessionRepository.getSessions();
    console.log('SESSIONS before set', session)
    await sessionRepository.setSessions(bets);
    let session2 = await sessionRepository.getSessions();
    console.log('SESSIONS after set', session2)

    // USERS
    let users = await userRepository.getUsers();
    console.log('USERS before set', users)
    let users2 = await userRepository.setUsers(users);    
    console.log('USERS after set', users2)

    // AUCTIONS
    let auctions = await auctionRepository.getAuctions();
    console.log('AUCTIONS after set', auctions)
    await auctionRepository.setAuctions(auctions);
    let auctions2 = await auctionRepository.getAuctions();
    console.log('AUCTIONS after set', auctions2)
}

getAndSetFromAllDatabases();