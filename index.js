const net = require('net')
const DatabaseControl = require('./database_control')

const databaseControl = new DatabaseControl().getInstance();
const databaseControl2 = new DatabaseControl().getInstance();


async function bootstrap(name) {
    let data = await databaseControl.getBets();
    console.log(data)
    await databaseControl.setBets({
        ['23ipo312pio']: data['23ipo312pio']
    });
    const data2 = await databaseControl2.getBets();
    data = await databaseControl.getBets();
    console.log(data)
    console.log(data2)
}

bootstrap('thiago');













// // ======================== TERMINAL SERVIDOR ====================
// // Esperando conexões








// // ======================== TERMINAL CLIENTE ====================
// // Conectar no servidor
// // Criar um leilão ou dar um lance
// // Dar um lance

// // Lista dos leilões
// 1 - Carro - 20.0
// 2 - Moto - 15.0
// 3 - Fogão - 200.0

// // Digite um leilao
// 1

// // Lista dos leilões
// 1 - Carro - 20.0

// // Digite o valor
// 20
