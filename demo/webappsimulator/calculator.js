const fs = require('fs');
const appLogger = require('./logging/app-logger'); // Get application logger

const getList = function getLogg(fileName) {
  let lines = [];
  try {
    lines = fs.readFileSync(fileName, 'utf8').split("\r\n"); // throws
  } catch (w) {
    appLogger.error(w.message);
  }
  console.log(`File ${fileName} loaded`);
  return lines;
};

const integers = function comparator(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
};

const numbers = getList('put-Copy.txt').map(s => parseInt(s)).sort(integers);
const len = numbers.length;
const sum = numbers.reduce((accumulator, currentValue) => accumulator + currentValue);
// 5 / 2 = 3 --> Math.ceil(len/2)
console.log('Median for datasett');
console.log(Math.ceil(len/2));
console.log('Gjennomsnittet for datasett');
console.log(sum/len);
