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

// const firstSchemaVersion = 'x';
const secondSchemaVersion = 'y';
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

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const main = async function migrationTest() {
  const countries = { 44: 'United Kingdom' , 61: 'Australia' };
  const addSemiColon = (str) => { if (str) return `${str}\n`; else return ''; };
  let i = 0;
  let j;
  let seed;
  let countryFlag;
  let len = 1019650;
  while (len > 0) {
    len = len - 200;
    console.log(len);
    i = 200;
    while (i > 0) {
      i = i - 1;
      console.log(i);
      countryFlag = Math.random() < 0.53;
      seed = countryFlag ? uk : aus;
      j = countryFlag ? 44 : 61;
      const httpPOSTBody = JSON.stringify({
        givenName: chooseRandom(seed.get('givenName')),
        surname:chooseRandom(seed.get('surname')),
        telephoneNumber: chooseRandom(seed.get('telephoneNumber')),
        address: `${addSemiColon(chooseRandom(seed.get('streetAddress')))}${addSemiColon(chooseRandom(seed.get('city')))}${addSemiColon(chooseRandom(seed.get('state')))}${addSemiColon(chooseRandom(aus.get('zipCode')))}${countries[j]}`,
        country: j,
      });
      fetch(`${chooseRandom(urlList)}?schema=${secondSchemaVersion}`, {
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
          } else {
            appLogger.warn(`POST Request failed: ${message}`);
          }
        })
        .catch(err => {
          appLogger.error(`POST Error: ${err.message}` )
        });
    }
    await sleep(1000);
  }
};


main().then();
