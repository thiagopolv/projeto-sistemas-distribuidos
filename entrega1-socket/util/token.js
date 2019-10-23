function generateToken(quantity) {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ123456790abcdefghijklmnopqrstuvwxyz';
    let token = '';
    for (let index = 0; index < quantity; index++) {
        token += alphabet[Math.floor(Math.random() * alphabet.length)];
    }
    return token
}

module.exports = generateToken;