// Logging using Winston
// Using winston-daily-rotate-file to create a new log file on a daily basis
// Winston log levels: { error: 0, warn: 1, info: 2, verbose: 3, debug: 4, silly: 5 }

const winston = require('winston');
require('winston-daily-rotate-file');

const config = require('./log-config.js');

winston.emitErrs = true;

const appLogger = new winston.Logger({
  transports: [
    new winston.transports.DailyRotateFile(config.application), // Log to file
  ],
  exitOnError: false,
});

module.exports = appLogger;

module.exports.infoStream = {
  write(message) {
    appLogger.info(message);
  },
};

module.exports.errorStream = {
  write(error) {
    appLogger.error(error);
  },
};
