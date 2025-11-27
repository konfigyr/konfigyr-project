import process from 'node:process';
import pino from 'pino';
import type { Logger } from 'pino';

const root = pino({
  level: process.env.LOG_LEVEL || 'debug',
});

/**
 * Creates a child logger with the given name that uses the server side configuration.
 *
 * @param name logger name
 * @returns {Logger} child logger
 */
export function createLogger(name: string): Logger {
  return root.child({ name });
}
