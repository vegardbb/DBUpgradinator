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

const firstSchemaVersion = 'y';
const secondSchemaVersion = 'z';
// FIXME: Change the urls in the list to match the IP addresses of the DO servers and get the port Spark listens to
const urlList = ["88.22.33.44:5001/api", "88.23.33.44:5002/api", "88.22.34.44:5003/api", "88.22.33.45:5004/api"];

const loadFile = function getSis(fileName) {
  const lines = fs.readFileSync(fileName, 'ascii').split("\r\n"); // throws
  const h = new Map();
  lines.forEach((l) => {
    const splitter = l.indexOf(",");
    const key = l.substring(0,splitter);
    h.set(key, l.substring(splitter).split(",").filter((thing, index, self) => self.indexOf(thing) === index));
  });
  return h;
};

// Inbefore calling this - use the vim editor to create the ids file
const getIdList = function getLogg(fileName) {
  let lines = [];
  try {
    lines = fs.readFileSync(fileName, 'ascii').split("\r\n"); // throws    
  } catch (Exception w) {
    appLogger.error(w.message);
  }
  return lines;
};

// Step 1 of test process: POST 6 GB worth of aggregates to database without migrating using the first schema
const partOne = function postProgram() {
  // Seed files for Australian and British accounts - Reads each of the two seed files into two different maps
  const aus = loadFile("./aus.csv");
  const uk = loadFile("./uk.csv");
  let httpBody;
  let seed;
  let countryFlag;
  let byteSize = 0;
  let pointer = 0; 
  while (byteSize < 8*10**9) {
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
        pointer = (pointer + 1) % 4;
      }).catch(err => appLogger.error(`Error ${err.status}: ${err.message}`));
  }
};

partOne();
// main();

// Actual testprogram
const main = function nodeProgram() {
  const persistedKeys = getIdList("./ids.txt"); // TODO: Implement this loading method
  // Common points of each part of testing script goes here
  const aus = loadFile("./aus.csv");
  const uk = loadFile("./uk.csv");
  const writeCallback = function afterWriteRequestOccured({ ok, message }, body) {
    if (ok) {
      appLogger.info(`Request AOK: ${message}`);
      byteSize = byteSize + getAggregateUTF8Size(body);
    } else {
      appLogger.info(`Request failed: ${message}`);
    }
      pointer = (pointer + 1) % 4;
  };
  const getCallback = function afterGetRequestOccured({ ok, message, aggregate }) {
    if (ok) {
      appLogger.info(`Request AOK: ${message}; aggregate: ${aggregate}`);
    } else {
      appLogger.info(`Request failed: ${message}`);
    }
      pointer = (pointer + 1) % 4;
  };
  let byteSize = 0;
  let pointer = 0;
  let map;
  let countryFlag;
  let id;
  while (persistedKeys.length > 0) {
    id = persistedKeys.pop();
    if (Math.random() < 0.3) {
      // Run PUT
      countryFlag = byteSize < 5*10**9;
      map = countryFlag ? uk : aus;
      putRequest(map, countryFlag ? 'UK' : 'AUS', `${urlList[pointer]}/${id}?schema=${secondSchemaVersion}`, 'PUT', writeCallback);
    } else {
      // Run GET
      getRequest(`${urlList[pointer]}/${id}?schema=${secondSchemaVersion}`, getCallback);
    }
    if (byteSize < 10**9) putRequest(aus, 'AUS', `${urlList[pointer]}?schema=${secondSchemaVersion}`, 'POST', writeCallback);
  }
};

// POST a migrated aggregate to database using the new schema - 3rd act of test
const putRequest = function writeItemProgram(seed, country, api, hm, caller) {
  const httpBody = JSON.stringify({
    givenName: chooseRandom(seed.get('givenName')),
    surname:chooseRandom(seed.get('surname')),
    telephoneNumber: chooseRandom(seed.get('telephoneNumber')),
    streetAddress: chooseRandom(seed.get('streetAddress')),
    city: chooseRandom(seed.get('city')),
    state: chooseRandom(seed.get('state')),
    zipCode: chooseRandom(seed.get('zipCode')),
    country, // : countryFlag ? 44 : 61,
  });
  fetch(api, { // 
    method: hm, // 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: httpBody,
  }).then(getJSONResponse)
    .then(({ ok, message }) => caller({ ok, message }, httpBody))
    .catch(err => appLogger.error(`Error ${err.status}: ${err.message}`));
};

// GET a previously persisted aggregate from database using the old schema
const getRequest = function writeItemProgram(api, caller) {
  fetch(api, { // `${urlList[pointer]}?schema=${secondSchemaVersion}`
    method: 'GET',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then(getJSONResponse)
    .then(caller)
    .catch(err => appLogger.error(`Error ${err.status}: ${err.message}`));
};
