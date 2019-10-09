module.exports = {
    server: {
        expireSessionMessage: '\nA sessão expirou',
        successAuthenticationMessage: '\nUsúario autenticado com sucesso.',
        errorAuthenticationMessage: '\nOcorreu um erro na autenticação.',
        successCreatingUser: '\nUsúario criado com sucesso.',
        errorCreatingUser: '\nUsúario já existente',
        endMessage: '\nO cliente encerrou a conexão',
        errorMessage: '\nErro no servidor.',
    },
    client: {
        successConnect: 'Conectado com sucesso ao servidor.\n',
        isRegister: '\nJá possui cadastro? (s ou n) ',
        findOrCreateAuction: '\nCRIAR ou BUSCAR um leilão? ',
        question: {
            auctionName: '\nNome do leilão? ',
            initialValue: '\nValor inicial? (ex: 10.00) ',
            minutes: '\nQuanto tempo durará o leilão? (em minutos) ',
        },
        auctionFinished: '\nO leilão terminou!',
        chooseAuction: '\nEscolha um leilão para participar: ',
        notAuctions: '\nNão há leilões ativos no momento',
        auctions: '\n\nLeilões: ',
        waitingPlayers: '\nEsperando mais jogadores para começar... ',
        exampleBid: '\nDê um lance: (ex: 10.00) ',
        finishedAuction: '\nO leilão foi finalizado!',
        serverClosed: '\nO servidor encerrou a conexão.',
        serverError: '\nErro no servidor.',
        serverNotAnswering: '\nO servidor não respondeu a conexão.',
        reconect: '\nEsperando para reconectar em 1s.',
        getUserMessage: '\nInsira seu usuário: ',
        getPasswordMessage: '\nInsira sua senha: ',
    }
};