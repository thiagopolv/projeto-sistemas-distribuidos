const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const DATABASE_BASE_PATH = __dirname + '/../../database'

class DatabaseRepository {

    async _getFromFile(databaseSpecificPath, memoryValue) {
        if (memoryValue) {
            return memoryValue;
        }
        try {
            const read = await readFile(this._resolveDatabasePath(databaseSpecificPath))
            memoryValue = JSON.parse(read.toString());
        } catch (e) {
            console.log(e)
            this._retryOperation(this._writeOnDatabase, memoryValue);
        }

        return memoryValue;
    }

    async _writeOnDatabase(databaseSpecificPath, memoryValue, newValue) {
        if (!!newValue) {
            try {
                memoryValue = newValue;
                await writeFile(this._resolveDatabasePath(databaseSpecificPath), JSON.stringify(memoryValue));
            }
            catch (e) {
                console.log(e)
                this._retryOperation(this._writeOnDatabase, databaseSpecificPath, memoryValue, newValue);
            }
        }
        return memoryValue;
    }

    _retryOperation(callback, ...object) {
        setTimeout(function () {
            callback(...object);
        }, 2000);
    }

    _resolveDatabasePath(db) {
        return `${DATABASE_BASE_PATH}/${db}.json`
    }
}

module.exports = DatabaseRepository;