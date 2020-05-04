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

const writeStuff = function writeToFile(fileName, str) {
  try {
    fs.writeFileSync(fileName, str); // throws
  } catch (w) {
    appLogger.error(w.message);
  }
  console.log(`File ${fileName} written`);
};

const strings = function comparator(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
};

// Step one: get list of date times, which can be sorted by string value
const hugeList = getList('put-times.txt');
const dates = [...new Set(hugeList)].sort(strings);
// Step two: Group the array using map on each unique string
const stringCount = new Map(dates.map(
  x => [x, hugeList.filter(y => y === x).length]
));

// Step three: find the earliest date by finding the lowest string value
const earliest = dates.reduce(function (pre, cur) {
  return pre > cur ? cur : pre;
});
const baseTime = Math.round(Date.parse(earliest)/1000)-1;
let columns = 't,f\r\n';
const keyValuePair = function logMapElements(value, key, map) {
  columns = `${columns}${Math.round(Date.parse(key)/1000)-baseTime},${value}\r\n`;
  // console.log([Math.round(Date.parse(key)/1000)-baseTime, value]);
}

stringCount.forEach(keyValuePair);
writeStuff('put.txt',columns);
