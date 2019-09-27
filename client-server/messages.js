const startingMessage = '\nNúmero mínimo de participantes atingido. Leilão iniciado!';
const connectedClientsMessage = 'Participantes conectados: {0}\n';
const connectionMessage = 'Um novo participante se conectou ao leilão.\n' + connectedClientsMessage;
const endMessage = 'O cliente encerrou a conexão';
const errorMessage = 'Erro no servidor.';
const startBidsMessage= '';
const clientConnectionError = 'Erro na conexão com o cliente.';

module.exports = {startingMessage, connectedClientsMessage, connectionMessage, endMessage, errorMessage, startBidsMessage, clientConnectionError};