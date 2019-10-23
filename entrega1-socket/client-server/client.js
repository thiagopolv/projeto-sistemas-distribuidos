const readline = require('readline');
const net = require('net');

const messages = require('./messages');
const actions = require('./actions');

const { negativeAnswers, clientPort, ipToConnect } = require('../config')

let blockRender = false;
let messageAfterClear = '';
let auctions = {};
let sessionAuction;
let sessionToken;
let clientSocket;
let readLineInterface;
let inputQuestion = promisifyInputQuestion();

async function connectToAuction() {
    clientSocket = createSocket();
    startConnection(clientPort, ipToConnect);
    onDataEvent();
    onErrorEvent();
    onEndEvent();
}

function createSocket() {
    const client = new net.Socket();
    return client;
}

function startConnection(serverPort, serverIP) {
    clientSocket.connect(serverPort, serverIP, () => {
        clearConsole();
        console.log(messages.client.successConnect)
        goToProgramMainState(true);
    });
}

function promisifyInputQuestion() {
    readLineInterface = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });
    return (question) => {
        return new Promise(async (resolve, reject) => {
            readLineInterface.question(question, (input) => {
                readLineInterface.close();
                resolve(input)
            })
        });
    }
}

function goToProgramMainState(dontClear, message) {
    if (!blockRender && !dontClear) {
        clearConsole();
        if (message !== '') {
            console.log(message)
            message = '';
        }
    }

    console.log(messageAfterClear)

    if (sessionToken && sessionAuction) {
        renderSingleAuction(sessionAuction);
        return;
    }
    if (sessionToken) {
        selectAuctionOrCreate();
        return;
    }
    authenticationProcess();
}

async function authenticationProcess() {
    const isNewUser = await inputQuestion(messages.client.isRegister);
    inputQuestion = promisifyInputQuestion();
    const data = await getAuthData();

    if (verifyNegativeAnswer(isNewUser)) {
        createUser(data);
        return;
    }
    login(data);
}

async function selectAuctionOrCreate() {
    blockRender = true;
    const option = await inputQuestion(messages.client.findOrCreateAuction);
    inputQuestion = promisifyInputQuestion();
    blockRender = false;
    if (option.toLowerCase() === 'criar') {
        await createAuction();
    }
    if (option.toLowerCase() === 'buscar') {
        getAvailableAuctions();
    }
}

function verifyNegativeAnswer(isUser) {
    return negativeAnswers.some(negativeAnswer => negativeAnswer === isUser.toLowerCase())
}

function createUser(messageToSent) {
    messageToSent.action = actions.auth.name;
    messageToSent.type = actions.auth.type.createUser;
    sendMessageToServer(clientSocket, messageToSent);
}

function login(messageToSent) {
    messageToSent.action = actions.auth.name;
    messageToSent.type = actions.auth.type.login;
    sendMessageToServer(clientSocket, messageToSent);
}

async function getAuthData() {
    const user = await inputQuestion(messages.client.getUserMessage)
    inputQuestion = promisifyInputQuestion();
    const password = await inputQuestion(messages.client.getPasswordMessage);
    inputQuestion = promisifyInputQuestion();

    return { data: { user, password } }
}

function sendMessageToServer(socket, data) {
    socket.write(JSON.stringify(data))
}

async function processAuthenticationAction(fullData) {
    switch (fullData.type) {
        case 'AUTH_SUCCESS':
            sessionToken = fullData.token;
            goToProgramMainState(false, fullData.data.message);
            break;
        case 'AUTH_FAIL':
        case 'TOKEN_EXPIRED':
            sessionToken = null;
            goToProgramMainState(false, fullData.data.message);
            break;
    }
}

function clearConsole() {
    console.log('\u001B[2J\u001B[0;0f');
}

function getAvailableAuctions() {
    const data = {
        action: actions.auction.name,
        type: actions.auction.type.get,
        token: sessionToken
    };
    sendMessageToServer(clientSocket, data)
}

async function createAuction() {
    blockRender = true;
    const name = await inputQuestion(messages.client.question.auctionName);
    inputQuestion = promisifyInputQuestion();
    const initialValue = await inputQuestion(messages.client.question.initialValue);
    inputQuestion = promisifyInputQuestion();
    const minutes = await inputQuestion(messages.client.question.minutes)
    inputQuestion = promisifyInputQuestion();

    const data = { name, initialValue, minutes }
    await sendMessageToServer(clientSocket, { data: data, action: actions.auction.name, type: actions.auction.type.create, token: sessionToken })
    blockRender = false;
}

async function processNotificationAction(fullData) {
    switch (fullData.type) {
        case 'NEW_USER_JOINNED':
            await processNewUserJoinned(fullData.data);
            break;
        case 'FINISHED_AUCTION':
            await processFinishedAuction(fullData.data);
            break;
    }
}

function processFinishedAuction({ auction }) {
    sessionAuction = null;
    blockRender = false;
    // console.log(messages.client.auctionFinished)
    // console.log();
    readLineInterface.close();
    readLineInterface = createInterface();
    goToProgramMainState(false, messages.client.auctionFinished +
        `\nO vencedor do leilão foi ${auction.winner} com o lance de ${auction.currentValue} finalizado ${auction.endDate}`);
}

function createInterface() {
    return readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });
}

function processNewUserJoinned(data) {
    console.log(`\n\nO usuário ${data.user} acabou de se conectar ao leilão ${data.auction.name}\n`)
    sessionAuction = data.auction;
}

async function processAuctionAction(fullData) {
    switch (fullData.type) {
        case 'GET_AUCTION':
            await doGetAuctionBusinessRules(fullData);
            break;
        case 'NEW_AUCTION':
            await doCreateAuctionBusinessRules(fullData);
            break;
        case 'AUCTION_ASSOCIATE_SUCCESS':
            processAuctionAssociation(fullData);
            goToProgramMainState(false);
            break;
        case 'AUCTION_ASSOCIATE_ERROR':
            processAssociationError(fullData);
            blockRender = false;
            goToProgramMainState(false, fullData.message);
            break;
        case 'SUCCESS_CREATE_AUCTION':
            processSuccessAuctionCreate(fullData);
            break;
        case 'ERROR_CREATE_AUCTION':
            processErrorAuctionCreate(fullData);
            break;
    }
}

async function doGetAuctionBusinessRules(fullData) {
    blockRender = false;
    const { isEmpty, message } = processNewAuctions(fullData);
    if (!isEmpty) {
        const auctionOption = await inputQuestion(messages.client.chooseAuction);
        inputQuestion = promisifyInputQuestion();
        associateOnAuction(auctionOption);
        return
    }
    blockRender = false;
    goToProgramMainState(false, message);
}

async function doCreateAuctionBusinessRules(fullData) {
    saveAndRenderNewAuctions(fullData);

    if (blockRender)
        return;

    const auctionOption = await inputQuestion(messages.client.chooseAuction);
    inputQuestion = promisifyInputQuestion();
    associateOnAuction(auctionOption);
}

function processSuccessAuctionCreate(fullData) {
    sessionToken = fullData.token;
}

function processErrorAuctionCreate(fullData) {
    sessionToken = fullData.token;
    goToProgramMainState();
}

function processNewAuctions(fullData) {
    sessionToken = fullData.token;
    return saveAndRenderNewAuctions(fullData);
}

function processAssociationError(fullData) {
    sessionToken = fullData.token;
}

function saveAndRenderNewAuctions(fullData) {
    auctions = fullData.data;
    return renderAuctions();
}

function renderAuctions() {
    let messageTosend = '';
    if (blockRender) {
        return { isEmpty: false };
    }
    const filterFinished = Object.values(auctions).filter((element) => {
        return element.status != 'FINISHED';
    })
    clearConsole();
    if (filterFinished.length) {
        messageTosend = messages.client.auctions
        filterFinished.forEach((element, index) => {
            printAuction(element, index);
        });
        return { isEmpty: false };
    }
    messageTosend = messages.client.notAuctions
    return { isEmpty: true, message: messageTosend };
}

function printAuction(element, index) {
    if (typeof index === 'number') {
        console.log(`${index}: ${element.name} | Valor atual: ${element.currentValue} | dono: ${element.owner} | ` +
            `status: ${element.status} |` + correctTimeShow(element))
        return;
    }
    console.log(`${element.name} | Valor atual: ${element.currentValue} | dono: ${element.owner} | ` +
        `status: ${element.status} |` + correctTimeShow(element))
}

function correctTimeShow(element) {
    if (element.endDate) {
        return ` Término: ${element.endDate}`;
    }
    return ` Duração: ${Number(element.minutes)} minuto` + (Number(element.minutes) > 1 ? 's' : '');
}

async function renderSingleAuction(data) {
    printAuction(data);
    if (data.status === 'WAITING_PLAYERS') {
        console.log(messages.client.waitingPlayers)
        return;
    }
    if (data.status === 'GOING_ON') {
        const bid = await inputQuestion(messages.client.exampleBid);
        inputQuestion = promisifyInputQuestion();
        sendMessageToServer(clientSocket, { data: { auction: sessionAuction, bid: Number(bid) }, token: sessionToken, action: actions.bid.name, type: actions.bid.type.new });
    }
    if (data.status === 'FINISHED') {
        sessionAuction = null;
        goToProgramMainState(true, messages.client.finishedAuction);
    }
}

function associateOnAuction(auctionOption) {
    const auctionToAssociate = Object.values(auctions).filter((element, index) => {
        return auctionOption === element.name || auctionOption == index
    })[0];
    blockRender = true;
    sendMessageToServer(clientSocket, { data: auctionToAssociate, action: actions.auction.name, type: actions.auction.type.associate, token: sessionToken })
}

function processAuctionAssociation(fullData) {
    sessionToken = fullData.token;
    sessionAuction = fullData.data.auction;
    blockRender = true;
}

async function processBidAction(fullData) {
    switch (fullData.type) {
        case 'UPDATED_BID':
            processUpdatedAuctionValue(fullData.data);
            break;
        case 'ERROR_BID':
            sessionToken = fullData.token;
            blockRender = false;
            goToProgramMainState(false, fullData.data.message);
            break;
        case 'SUCESS_NEW_BID':
            blockRender = false;
            processSuccessNewBid(fullData);
            break;
    }
}

function processSuccessNewBid(fullData) {
    sessionToken = fullData.token;
    sessionAuction = fullData.data.auction;
}

function processUpdatedAuctionValue(data) {
    sessionAuction = data.auction;
    goToProgramMainState(false, '');
}

function onDataEvent() {
    clientSocket.on('data', processData);
}

async function processData(dataFromBuffer) {
    const fullData = JSON.parse(dataFromBuffer)
    // clearConsole();
    switch (fullData.action) {
        case 'AUTH':
            await processAuthenticationAction(fullData);
            break;
        case 'NOTIFICATION':
            await processNotificationAction(fullData);
            break;
        case 'AUCTION':
            await processAuctionAction(fullData);
            break;
        case 'BID':
            await processBidAction(fullData);
        default:
            break;
    }

}

function onEndEvent() {
    clientSocket.on('end', function () {
        console.log(messages.client.serverClosed)
    });
}

function onErrorEvent() {
    clientSocket.on('error', function (err) {
        if (err.errno === 'ECONNREFUSED' || err.errno === 'ECONNRESET') {
            clientSocket.destroy();
            waitToReconnect();
            return;
        }
        console.log(err)
        console.log(messages.client.serverError)
        process.exit(1);
    });
}

function waitToReconnect() {
    console.log(messages.client.serverNotAnswering)
    console.log(messages.client.reconect)
    setTimeout(() => {
        connectToAuction();
    }, 1000);
}

connectToAuction();