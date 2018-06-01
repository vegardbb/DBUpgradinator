const path = require('path');

// Configs for logging with the Winston module
// Defines different configs for development, testing, and production

// Custom timestamp function
const localTime = () => (new Date()).toISOString();

const config = Object.freeze(
  {
    application: {
      filename: path.join(__dirname, '../', 'logs', 'log'), // Prepend log to filename
      datePattern: 'yyyy-MM-dd.',
      prepend: true,
      localTime: true,
      level: 'debug',
      handleExceptions: true,
      json: true,
      maxsize: 1024 * 1024 * 100000,
      maxFiles: 7,
      colorize: false,
      timestamp: localTime,
    },
    console: {
      level: 'debug',
      handleExceptions: true,
      json: false,
      colorize: true,
      timestamp: localTime,
    },
  }
);

module.exports = config;
