// A node program used to send HTTP requests

const fs = require('fs');
const fetch = require('isomorphic-fetch');
const appLogger = require('./logging/app-logger'); // Get application logger

// Alternative: str => 2 * str.length; here we estimate the size of the UTF8 encoded string
const getAggregateUTF8Size = str => ~-encodeURI(str).split(/%..|./).length;

const chooseRandom = myArray => myArray[Math.floor(Math.random() * myArray.length)];

// Throws Error
const getJSONResponse = function uponReceiving(res) {
    if (res.ok && res.headers.get('content-type').match(/application\/json/)) return res.json();
    throw new Error('Server response was not ok.');
};

const firstSchemaVersion = 'x';
const secondSchemaVersion = 'y';
// FIXME: Change the urls in the list to match the IP addresses of the DO servers and get the port Spark listens to
const urlList = ["167.99.84.243:5001/api", "188.166.103.205:5002/api", "206.81.31.127:5003/api", "206.81.31.128:5004/api"];

const loadFile = function getSis(fileName) {
    const lines = fs.readFileSync(fileName, 'ascii').split("\r\n"); // throws
    const h = new Map();
    lines.forEach((l) => {
        const splitter = l.indexOf(",");
        const key = l.substring(0,splitter);
        h.set(key, l.substring(splitter).split(",").filter((thing, index, self) => self.indexOf(thing) === index));
    });
    console.log(`File ${filename} loaded`);
    return h;
};

// Inbefore calling this - use the vim editor to create the ids file
const getIdList = function getLogg(fileName) {
    let lines = [];
    try {
        lines = fs.readFileSync(fileName, 'ascii').split("\r\n"); // throws
    } catch (w) {
        appLogger.error(w.message);
    }
    console.log(`File ${filename} loaded`);
    return lines;
};

// GET a previously persisted aggregate from database using the old schema
const getRequest = function readItemProgram(api, caller) {
    fetch(api, { // `${urlList[pointer]}?schema=${secondSchemaVersion}`
        method: 'GET',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
        },
    }).then(getJSONResponse)
        .then(caller)
        .catch(err => appLogger.error(`GET Error ${err.status}: ${err.message}`));
};

// Step 1 of test process: POST 6 GB worth of aggregates to database without migrating using the first schema
const partOne = function postProgram() {
    // Seed files for Australian and British accounts - Reads each of the two seed files into two different maps
    const aus = loadFile("./aus.csv");
    const uk = loadFile("./uk.csv");
    let httpBody;
    let seed;
    let countryFlag;
    let connections = 0;
    let byteSize = 0;
    let pointer = 0;
    while (byteSize < 8*10**9 && connections < 180) {
        countryFlag = byteSize < 4*10**9;
        seed = countryFlag ? uk : aus;
        httpBody = JSON.stringify({
            givenName: chooseRandom(seed.get('givenName')),
            surname:chooseRandom(seed.get('surname')),
            telephoneNumber: chooseRandom(seed.get('telephoneNumber')),
            streetAddress: chooseRandom(seed.get('streetAddress')),
            city: chooseRandom(seed.get('city')),
            state: chooseRandom(seed.get('state')),
            zipCode: chooseRandom(seed.get('zipCode')),
            country: countryFlag ? 'UK' : 'AUS',
        });
        connections = connections + 1;
        fetch(`${urlList[pointer]}?schema=${firstSchemaVersion}`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            body: httpBody,
        }).then(getJSONResponse)
            .then(({ ok, message }) => {
                if (ok) {
                    appLogger.info(`Request AOK: ${message}; POST AOK: ${httpBody}`);
                    byteSize = byteSize + getAggregateUTF8Size(httpBody);
                } else {
                    appLogger.info(`Request failed: ${message}`);
                }
                console.log(`POST finished. Result: ${ok}`);
                connections = connections - 1;
                pointer = (pointer + 1) % 4;
            }).catch(err => {
            connections = connections - 1;
            appLogger.error(`Error ${err.status}: ${err.message}` )
        });
    }
};

// Step 2 of test process: Migrate each of aggregates to database without migrating using the first schema
const partTwo = function migrateProgram() {
    const countries = { 44: 'United Kingdom' , 61: 'Australia' };
    const addSemiColon = (str) => { if (str) return `${str}\n`; else return ''; };
    const persistedKeys = getIdList("./ids.txt");
    // Common points of each part of testing script goes here
    const aus = loadFile("./aus.csv");
    const uk = loadFile("./uk.csv");
    let connections = 0;
    const getCallback = function afterGetRequestOccurred({ ok, message, aggregate }) {
        if (ok) {
            appLogger.info(`GET Request AOK: ${message}; aggregate: ${aggregate}`);
            console.log(`GET finished. Aggregate: ${aggregate}`);
        } else {
            appLogger.info(`GET Request failed: ${message}`);
        }
        pointer = (pointer + 1) % 4;
        connections = connections - 1;
    };
    let byteSize = 0;
    let pointer = 0;
    let seed;
    let httpBody;
    let countryFlag;
    let id;
    while (persistedKeys.length > 0 && connections < 180) {
        id = persistedKeys.pop();
        if (Math.random() < 0.3) {
            // Run PUT
            countryFlag = byteSize < 4*10**9;
            seed = countryFlag ? uk : aus;
            // PUT request using the old schema, ie, updating an aggregate in the previous schema version
            httpBody = JSON.stringify({
                givenName: chooseRandom(seed.get('givenName')),
                surname:chooseRandom(seed.get('surname')),
                telephoneNumber: chooseRandom(seed.get('telephoneNumber')),
                streetAddress: chooseRandom(seed.get('streetAddress')),
                city: chooseRandom(seed.get('city')),
                state: chooseRandom(seed.get('state')),
                zipCode: chooseRandom(seed.get('zipCode')),
                country: countryFlag ? 'UK' : 'AUS',
            });
            connections = connections + 1;
            fetch(`${urlList[pointer]}/${id}?schema=${firstSchemaVersion}`, {
                method: 'PUT',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: httpBody,
            }).then(getJSONResponse)
                .then(({ ok, message }) => {
                    if (ok) {
                        appLogger.info(`PUT Request AOK: ${message}; Aggregate: ${httpBody}`);
                        byteSize = byteSize + getAggregateUTF8Size(httpBody);
                        connections = connections + 1;
                        getRequest(`${urlList[((pointer + 3) % 4)]}/${id}?schema=${secondSchemaVersion}`, getCallback);
                    } else {
                        appLogger.info(`PUT Request failed: ${message}`);
                    }
                    console.log(`PUT finished. Result: ${ok}`);
                    pointer = (pointer + 1) % 4;
                    connections = connections - 1
                }).catch(err => {
                connections = connections - 1;
                appLogger.error(`PUT Error ${err.status}: ${err.message}` )
            });
        } else {
            // Run GET
            getRequest(`${urlList[pointer]}/${id}?schema=${firstSchemaVersion}`, getCallback);
        }
        if (byteSize < 10**9) {
            const httpPOSTBody = JSON.stringify({
                givenName: chooseRandom(aus.get('givenName')),
                surname:chooseRandom(aus.get('surname')),
                telephoneNumber: chooseRandom(aus.get('telephoneNumber')),
                address: `${addSemiColon(chooseRandom(aus.get('streetAddress')))}${addSemiColon(chooseRandom(aus.get('city')))}${addSemiColon(chooseRandom(aus.get('state')))}${addSemiColon(chooseRandom(aus.get('zipCode')))}${countries[61]}`,
                country: 61,
            });
            connections = connections + 1;
            fetch(`${urlList[((pointer + 2) % 4)]}?schema=${secondSchemaVersion}`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: httpPOSTBody,
            }).then(getJSONResponse)
                .then(({ ok, message }) => {
                    if (ok) {
                        appLogger.info(`POST Request AOK: ${message}`);
                        byteSize = byteSize + getAggregateUTF8Size(httpPOSTBody);
                    } else {
                        appLogger.info(`POST Request failed: ${message}`);
                    }
                    connections = connections - 1;
                    pointer = (pointer + 1) % 4;
                    console.log('New POST to DB');
                })
                .catch(err => {
                    connections = connections - 1;
                    appLogger.error(`POST Error ${err.status}: ${err.message}` )
                });
        }
    }
};

partOne();
// partTwo();
