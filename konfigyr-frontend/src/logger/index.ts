import pino from 'pino';
import {
  printLoggerEvent,
  resolveLoggerLevel,
} from './browser';

import type { Logger } from 'pino';
import type { LoggerEvent } from './browser';

const root = pino({
  level: 'debug',
  browser: {
    serialize: true,
    asObject: true,
    formatters: {
      level: resolveLoggerLevel,
    },
    write: (o) => printLoggerEvent(o as LoggerEvent),
  },
});

/**
 * Creates a child logger with the given name that is based on the browser native console logger
 * with custom message formatting.
 *
 * @param name logger name
 * @returns {Logger} child logger
 */
export function createLogger(name: string): Logger {
  return root.child({ name });
}
