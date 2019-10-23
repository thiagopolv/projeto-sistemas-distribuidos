const readline = require('readline').createInterface({
    input: process.stdin,
    output: process.stdout 
 });
 
 
 async function a() { 
    let bid = await readline.question('Insira o seu lance: ', (bid) => {
        console.log(bid);
    });
 }
 a();