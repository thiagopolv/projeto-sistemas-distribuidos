const readline = require('readline');
const net = require('net');

const messages = require('./messages');
const actions = require('./actions');
let inputQuestion = promisifyInputQuestion();

const readLineInterface = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

const negativeAnswers = ['no', 'n', 'nao', 'não'];
const randomPort = 3000;
const randomIP = 'localhost';

let blockRender = false;
let auctions = {};
let sessionAuction;
let sessionToken;
let clientSocket;

async function connectToAuction() {
    clientSocket = createSocket();
    startConnection(randomPort, randomIP);
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
        console.log('Conectado com sucesso ao servidor.\n')
        goToProgramMainState();
    });
}

function promisifyInputQuestion() {
    return (question) => {
        return new Promise((resolve, reject) => {
            readLineInterface.question(question, (input) => resolve(input))
        });
    }
}

function goToProgramMainState(dontClearConsole) {
    if (!dontClearConsole) {
        // clearConsole();
    }

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
    const isNewUser = await inputQuestion('\nJá possui cadastro? ');
    const data = await getAuthData();

    if (verifyNegativeAnswer(isNewUser)) {
        createUser(data);
        return;
    }
    login(data);
}

async function selectAuctionOrCreate() {
    blockRender = true;
    const option = await inputQuestion('\nCRIAR ou BUSCAR um leilão? ');
    blockRender = false;
    if (option === 'CRIAR') {
        createAuction();
    }
    getAvailableAuctions();
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
    const user = await inputQuestion(messages.getUserMessage)
    const password = await inputQuestion(messages.getPasswordMessage);

    return { data: { user, password } }
}

function sendMessageToServer(socket, data) {
    socket.write(JSON.stringify(data))
}

function processAuthenticationAction(fullData) {
    switch (fullData.type) {
        case 'AUTH_SUCCESS':
            console.log(fullData.data.message)
            sessionToken = fullData.token;
            goToProgramMainState();
            break;
        case 'AUTH_FAIL':
        case 'TOKEN_EXPIRED':
            console.log(fullData.data.message)
            sessionToken = null;
            authenticationProcess();
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
    const data = {
        name: await inputQuestion('\nNome do leilão? '),
        initialValue: await inputQuestion('\nValor inicial? (ex: 10.00) '),
        minutes: await inputQuestion('\nQuanto tempo durará o leilão? (em minutos) ')
    }
    await sendMessageToServer(clientSocket, { data: data, action: actions.auction.name, type: actions.auction.type.create, token: sessionToken })
    blockRender = false;
}

function processNotificationAction(fullData) {
    switch (fullData.type) {
        case 'NEW_USER_JOINNED':
            processNewUserJoinned(fullData.data);
            break;
        case 'FINISHED_AUCTION':
            console.log('\nO leilão terminou!')
            processFinishedAuction(fullData.data);
            goToProgramMainState();
            break;
    }
}

function processFinishedAuction({ auction }) {
    sessionAuction = null;
    blockRender = false;
    console.log(`\nO vencedor do leilão foi ${auction.winner} com o lance de ${auction.currentValue} finalizado ${auction.endDate}`);
    readLineInterface.close();
    inputQuestion = promisifyInputQuestion();
}

function processNewUserJoinned(data) {
    console.log(`\n\nO usuário ${data.user} acabou de se conectar ao leilão ${data.auction.name}\n`)
    sessionAuction = data.auction;
    goToProgramMainState();
}

function processAuctionAction(fullData) {
    switch (fullData.type) {
        case 'GET_AUCTION':
            doGetAuctionBusinessRules(fullData);
            break;
        case 'NEW_AUCTION':
            doCreateAuctionBusinessRules(fullData);
            break;
        case 'AUCTION_ASSOCIATE_SUCCESS':
            processAuctionAssociation(fullData);
            goToProgramMainState();
            break;
        case 'AUCTION_ASSOCIATE_ERROR':
            console.log(fullData.message)
            processAssociationError(fullData);
            goToProgramMainState();
            blockRender = false;
            break;
        case 'SUCCESS_CREATE_AUCTION':
            processSuccessAuctionCreate(fullData);
            break;
    }
}

async function doGetAuctionBusinessRules(fullData) {
    blockRender = false;
    const isEmpty = processNewAuctions(fullData);
    if (!isEmpty) {
        const auctionOption = await inputQuestion('\nEscolha um leilão para participar: ');
        associateOnAuction(auctionOption);
        return
    }
    goToProgramMainState();
}

async function doCreateAuctionBusinessRules(fullData) {
    saveAndRenderNewAuctions(fullData);

    if (blockRender)
        return;

    const auctionOption = await inputQuestion('\nEscolha um leilão para participar: ');
    associateOnAuction(auctionOption);
}

function processSuccessAuctionCreate(fullData) {
    sessionToken = fullData.token;
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
    if (blockRender) {
        return;
    }
    const filterFinished = Object.values(auctions).filter((element) => {
        return element.status != 'FINISHED';
    })
    // clearConsole();
    console.log('\n\nLeilões: ')
    if (!filterFinished.length) {
        console.log('\nNão há leilões ativos no momento')
        return true;
    }
    filterFinished.forEach((element, index) => {
        printAuction(element, index);
    });
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
        console.log('\nEsperando mais jogadores para começar... ')
        return;
    }
    if (data.status === 'GOING_ON') {
        const bid = await inputQuestion('\nDê um lance: (ex: 10.00) ');
        sendMessageToServer(clientSocket, { data: { auction: sessionAuction, bid: Number(bid) }, token: sessionToken, action: actions.bid.name, type: actions.bid.type.new });
    }
    if (data.status === 'FINISHED') {
        console.log('\nO leilão foi finalizado!')
        sessionAuction = null;
        goToProgramMainState(true);
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

function processBidAction(fullData) {
    switch (fullData.type) {
        case 'UPDATED_BID':
            processUpdatedAuctionValue(fullData.data);
            break;
        case 'ERROR_BID':
            sessionToken = fullData.token;
            console.log(fullData.data.message)
            goToProgramMainState(true);
            break;
        case 'SUCESS_NEW_BID':
            processSuccessNewBid(fullData);
            break;
    }
}

function processSuccessNewBid(fullData) {
    sessionToken = fullData.token;
    processUpdatedAuctionValue(fullData.data);
}

function processUpdatedAuctionValue(data) {
    sessionAuction = data.auction;
    goToProgramMainState();
}

function onDataEvent() {
    clientSocket.on('data', function (dataFromBuffer) {
        const fullData = JSON.parse(dataFromBuffer)
        // clearConsole();
        switch (fullData.action) {
            case 'AUTH':
                processAuthenticationAction(fullData);
                break;
            case 'NOTIFICATION':
                processNotificationAction(fullData);
                break;
            case 'AUCTION':
                processAuctionAction(fullData);
                break;
            case 'BID':
                processBidAction(fullData);
            default:
                break;
        }
    });
}

function onEndEvent() {
    clientSocket.on('end', function () {
        console.log('\nO servidor encerrou a conexão.')
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
        console.log('\nErro no servidor.')
        process.exit(1);
    });
}

function waitToReconnect() {
    console.log('\nO servidor não respondeu a conexão.')
    console.log('\nEsperando para reconectar em 1s.')
    setTimeout(() => {
        connectToAuction();
    }, 1000);
}

connectToAuction();