const startingMessage = '\nNúmero mínimo de participantes atingido. Leilão iniciado!';
const connectedClientsMessage = '\nParticipantes conectados: {0}';
const connectionMessage = '\nUm novo participante se conectou ao leilão.' + connectedClientsMessage;
const endMessage = '\nO cliente encerrou a conexão';
const errorMessage = '\nErro no servidor.';
const clientConnectionError = '\nErro na conexão com o cliente.';
const sentBidMessage = 'Foi enviado um lance de {}.\nAguardando novo lance.\n\n';
const finishedAuctionMessage = '\n\nLeilão finalizado! O lance vencedor foi de R${}.\n';
const receivedBidErrorMessage = '\nO lance de R${} recebido é menor que o lance corrente.'
const sentBidErrorMessage = '\nO lance enviado é menor que o lance corrente.'
const sentBidFormatErrorMessage = '\nO valor enviado no lance deve ser numérico e possuir no máximo duas casas decimais.';

module.exports = {startingMessage, connectedClientsMessage, connectionMessage, endMessage, errorMessage, 
    clientConnectionError, sentBidMessage, finishedAuctionMessage, receivedBidErrorMessage, sentBidErrorMessage,
    sentBidFormatErrorMessage};