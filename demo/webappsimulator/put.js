// A node program used to send HTTP requests

const fs = require('fs');
const fetch = require('isomorphic-fetch');
const appLogger = require('./logging/app-logger'); // Get application logger

const chooseRandom = myArray => myArray[Math.floor(Math.random() * myArray.length)];

// Throws Error
const getJSONResponse = function uponReceiving(res) {
  if (res.ok && res.headers.get('content-type').match(/application\/json/)) return res.json();
  throw new Error('Server response was not ok.');
};

const firstSchemaVersion = 'x';
// const secondSchemaVersion = 'y';
const urlList = ["http://167.99.84.243:5001/api", "http://188.166.103.205:5002/api", "http://206.81.31.127:5003/api", "http://206.81.31.128:5004/api"];

const loadFile = function getSis(fileName) {
  const lines = fs.readFileSync(fileName, 'ascii').split("\r\n"); // throws
  const h = new Map();
  lines.forEach((l) => {
    const splitter = l.indexOf(",");
    const key = l.substring(0,splitter);
    const longList = [...new Set(l.substring(splitter).split(","))].filter(Boolean);
    if (longList.length === 0) longList.push('');
    h.set(key, longList);
  });
  console.log(`File ${fileName} loaded`);
  return h;
};
// Seed files for Australian and British accounts - Reads each of the two seed files into two different maps
const aus = loadFile("aus.csv");
const uk = loadFile("uk.csv");

const getIdList = function getLogg(fileName) {
  let lines = [];
  try {
    lines = fs.readFileSync(fileName, 'utf8').split("\r\n"); // throws
  } catch (w) {
    appLogger.error(w.message);
  }
  console.log(`File ${fileName} loaded`);
  return lines;
};

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const allKeys = getIdList("id.txt");

const main = async function migrationTest() {
    let i = 0;
    let j = 200;
    let persistedKeys;
    let id;
    let httpBody;
    let seed;
    let countryFlag;
    const len = allKeys.length;
    while (i < len) {
        console.log(`Element ${i} through ${j}`);
        persistedKeys = allKeys.slice(i,j);
        while (persistedKeys.length > 0) {
            id = persistedKeys.pop();
            countryFlag = Math.random() < 0.53;
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
            fetch(`${chooseRandom(urlList)}/${id}??schema=${firstSchemaVersion}`, {
                method: 'PUT',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: httpBody,
            }).then(getJSONResponse)
                .then(({ ok, message }) => {
                    if (ok) {
                        appLogger.info(`Request AOK: ${message}`);
                    } else {
                        appLogger.warn(`Request failed: ${message}`);
                    }
                }).catch(err => {
                appLogger.error(`Error: ${err.message}` )
            });
        }
        await sleep(1000); // Stops while loop from starting next round before
        i = j;
        j = j + 200;
    }
};

main().then();
