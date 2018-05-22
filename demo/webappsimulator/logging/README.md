# Logging with Node.js
It is important to make sure logging is done properly throughout the application. Simple solutions such as console.log is not sufficient. The logger.js is a good staring point, and can be configured further for effective logging.

## Todo
* What are appropriate log sizes?
* What are appropriate log formats?
* Use the logger throughout the application (not console.log(...))

## Known issues
* localTime function not calculating local time (-2 hours)

## Logging requirements
1. Timestamps
Know when an event has happened.

2. Logging format
Write readable logs. You wan't to be able to quickly understand what is going on,
while the machine have to parse the file as well.

3. Log destinations
The target should always be the standard output/error. No multi-transport logging,
as it is not the requirement of the application to route logs.

4. Support for log levels
Log events can have different severity levels - in some cases, you just want to log
events with at least a warning level, sometimes log lines have to be more verbose.

## Three areas of Logging
* when you are building a node module,
* when you are building an application,
* when you are building a distributed system.

### Logging in Node modules
It is not recommended to pollute the logs files with events from all kinds of Node modules you build.
However, when you are developing and need a better understand why a given issue rose, using a debug tool
is recommended. The Debug or Node Inspector module can be used for debugging node modules.

### Logging in you applications
Logging in the application can be done more sophistically.
The Winston and Morgan modules are recommendations here.
Never (!) log passwords, credentials or other private information.

### Logging in distributed systems
Not relevant as of now.

## Logging with Morgan and Winston modules
* Use winston to support different log levels and add transports to save logs to different log destinations
* Use morgan to log http requests, which logs are then handled by winston
* Timestamps are added by winston
* Both winston and morgan allow different log formats
