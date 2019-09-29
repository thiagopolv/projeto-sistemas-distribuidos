const readline = require('readline');
const net = require('net');

const messages = require('./messages');
const actions = require('./actions');
const inputQuestion = promisifyInputQuestion();

const negativeAnswers = ['no', 'n', 'nao', 'não'];
const randomPort = 3000;
const randomIP = 'localhost';
let auctionState = 'STARTING';

let blockRender = false;
let auctions = [];
let sessionAuction;
let sessionToken;
let clientSocket;

async function connectToAuction() {
    clientSocket = createSocket();
    startConnection(randomPort, randomIP);
    onDataEvent();
    onErrorEvent();
    onEndEvent();
    // onCloseEvent(client);
}

function createSocket() {
    const client = new net.Socket();
    return client;
}

function startConnection(serverPort, serverIP) {
    clientSocket.connect(serverPort, serverIP, async () => {
        clearConsole();
        console.log('Conectado com sucesso ao servidor.\n')
        if (sessionToken && sessionAuction) {
            // const option = await inputQuestion('\n 0: CRIAR ou 1: BUSCAR um leilão? ');
            // if (option === 0 || option === 'CRIAR') {
            //     return createAuction(client, sessionToken);
            // } else {
            //     return getAvailableAuctions(client, sessionToken);
            // }
        }
        if (sessionToken) {
            const option = await inputQuestion('\n 0: CRIAR ou 1: BUSCAR um leilão? ');
            if (option === 0 || option === 'CRIAR') {
                return createAuction(sessionToken);
            } else {
                return getAvailableAuctions(sessionToken);
            }
        }
        authenticationProcess();
    });
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

function verifyNegativeAnswer(isUser) {
    return negativeAnswers.some(negativeAnswer => negativeAnswer === isUser.toLowerCase())
}

function createUser(messageToSent) {
    console.log('Creating User')
    messageToSent.action = actions.auth.name;
    messageToSent.type = actions.auth.type.createUser;
    sendMessageToServer(clientSocket, messageToSent);
}

function login(messageToSent) {
    console.log('Login')
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

function processAuthenticationAction(data) {
    switch (data.type) {
        case 'AUTH_SUCCESS':
            console.log(data.data.message)
            sessionToken = data.token;
            getAvailableAuctions();
            break;
        case 'AUTH_FAIL':
        case 'TOKEN_EXPIRED':
            console.log(data.data.message)
            authenticationProcess();
            break;
    }
}

function clearConsole() {
    console.log('\u001B[2J\u001B[0;0f');
}

function getAvailableAuctions(sessionToken) {
    const data = {
        action: actions.auction.name,
        type: actions.auction.type.get,
        token: sessionToken
    };
    console.log('Available auctions')
    sendMessageToServer(clientSocket, data)
}

async function createAuction(sessionToken) {
    blockRender = true;
    const data = {
        name: await inputQuestion('\nNome do leilão? '),
        initialValue: await inputQuestion('\nValor inicial? (ex: 10.00) '),
        endDate: await inputQuestion('\nHorário de término? (ex: YYYY-MM-DD HH:mm) '),
        token: sessionToken
    }
    await sendMessageToServer(clientSocket, data)
    blockRender = false;
}

function processNotificationAction(data) {
    switch (data.type) {
        case '':
    }
    console.log(`\nRecebido: ${data}`);
    if (data == messages.startingMessage) {
        auctionState = 'STARTED';
    }

    if (data == messages.sentBidErrorMessage) {
        console.log(messages.sentBidErrorMessage);
    }

    if (auctionState == 'STARTED') {
        sendBid(clientSocket);
    }
}

function processAuctionAction(fullData) {
    switch (fullData.type) {
        case 'GET_AUCTION':
            blockRender = false;
            processNewAuctions(fullData.data);
            break;
        case 'NEW_AUCTION':
            renderAuctions();
            break;
        case 'AUCTION_ASSOCIATE':
            break;
    }
}

function processNewAuctions(data) {
    sessionToken = data.token;
    auctions = data;
    renderAuctions();
}

function renderAuctions() {
    if (blockRender) {
        return;
    }
    Object.values(auctions).forEach((element, index) => {
        console.log(`${index}: ${element.name} | currentValue: ${element.currentValue} | owner: ${element.owner} | ` +
            `status: ${element.status} | finish: ${element.endDate}\n`)
    });
}

function sendBid(socket) {
    question.question('\nInsira o seu lance: ', (bid) => {
        socket.write(bid);
    });
}

function promisifyInputQuestion() {
    const readLineInterface = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });
    return (question) => {
        return new Promise((resolve, reject) => {
            readLineInterface.question(question, (input) => resolve(input))
        });
    }
}

function onDataEvent() {
    clientSocket.on('data', function (data) {
        data = JSON.parse(data)
        console.log(data)
        // clearConsole();
        switch (data.action) {
            case 'AUTH':
                processAuthenticationAction(data);
                break;
            case 'NOTIFICATION':
                processNotificationAction(data);
                break;
            case 'AUCTION':
                processAuctionAction(data);
                break;
            default:
                break;
        }
    });
}

function onCloseEvent(socket) {
    socket.on('close', function (err) {
        console.log(err)
        console.log('\nConexão encerrada.');
        // socket.destroy();
        // waitToReconnect();
    });
}

function onEndEvent() {
    clientSocket.on('end', function () {
        console.log('\nO servidor encerrou a conexão.')
        // socket.destroy();
        // process.exit(1);
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