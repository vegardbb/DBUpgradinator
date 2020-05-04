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

// Inbefore calling this - use the vim editor to create the ids file
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

// GET a previously persisted aggregate from database using the old schema
const getRequest = function readItemProgram(api, caller) {
  fetch(api, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then(getJSONResponse)
    .then(caller)
    .catch(err => appLogger.error(`GET Error: ${err.message}`));
};

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

// This function bulk loads 5000 ids at the time.
const partTwo = function migrateProgram(persistedKeys) {
  const countries = { 44: 'United Kingdom' , 61: 'Australia' };
  const addSemiColon = (str) => { if (str) return `${str}\n`; else return ''; };
  // Common points of each part of testing script goes here
  const getCallback = function afterGetRequestOccurred({ ok, message, aggregate }) {
    if (ok) {
      appLogger.info(`GET Request AOK: ${message}; aggregate: ${aggregate}`);
    } else {
      appLogger.warn(`GET Request failed: ${message}`);
    }
  };
  let seed;
  let httpBody;
  let countryFlag;
  let id;
  let byteSize = 0;
  while (persistedKeys.length > 0) {
    id = persistedKeys.pop();
    if (Math.random() < 0.3) {
      // Run PUT
      countryFlag = byteSize < 5*10**5;
      seed = countryFlag ? uk : aus;
      // PUT request using the old schema, ie, updating an aggregate in the previous schema version -
      // ALL attributes are randomly changed
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
      byteSize = byteSize + getAggregateUTF8Size(httpBody);
      fetch(`${chooseRandom(urlList)}/${id}?schema=${firstSchemaVersion}`, {
        method: 'PUT',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: httpBody,
      }).then(getJSONResponse)
        .then(({ ok, message }) => {
          if (ok) {
            appLogger.info(`PUT Request AOK: ${message};`);
            getRequest(`${chooseRandom(urlList)}/${id}?schema=${secondSchemaVersion}`, getCallback);
          } else {
            appLogger.info(`PUT Request failed: ${message}`);
          }
        }).catch(err => {
        appLogger.error(`PUT Error: ${err.message}` )
      });
    } else {
      // Run GET
      getRequest(`${chooseRandom(urlList)}/${id}?schema=${firstSchemaVersion}`, getCallback);
    }
    if (byteSize < 10**6) {
      const httpPOSTBody = JSON.stringify({
        givenName: chooseRandom(aus.get('givenName')),
        surname:chooseRandom(aus.get('surname')),
        telephoneNumber: chooseRandom(aus.get('telephoneNumber')),
        address: `${addSemiColon(chooseRandom(aus.get('streetAddress')))}${addSemiColon(chooseRandom(aus.get('city')))}${addSemiColon(chooseRandom(aus.get('state')))}${addSemiColon(chooseRandom(aus.get('zipCode')))}${countries[61]}`,
        country: 61,
      });
      byteSize = byteSize + getAggregateUTF8Size(httpPOSTBody);
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
  }
};

// partOne();
const allKeys = getIdList("id.txt");
const main = async function migrationTest() {
  let i = 0;
  let j = 5000;
  const len = allKeys.length;
  while (i < len) {
    console.log(`Element ${i} through ${j}`);
    partTwo(allKeys.slice(i,j));
    await sleep(15000); // Stops while loop from starting next round before
    i = j;
    j = j + 5000;
  }
};

main();
