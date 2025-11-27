import pino from 'pino';
import type { Logger } from 'pino';

interface LoggerEvent {
  name: string,
  time: number,
  level: string,
  msg: string,
  stack?: string,
}

const root = pino({
  level: 'debug',
  browser: {
    serialize: true,
    asObject: true,
    formatters: {
      level: (label) => {
        const level = label === 'trace' ? 'debug' : label;
        return { level };
      },
    },
    write: (o) => {
      const event = o as LoggerEvent;
      const timestamp = new Date(event.time);
      const args = [
        `%c${timestamp.toLocaleTimeString()} %c${event.level.toUpperCase()} %c[${event.name}] %c${event.msg}`,
        'color: oklch(0.373 0.034 259.733);',
        'color: oklch(0.216 0.006 56.043); font-weight: bold;',
        'color: oklch(0.546 0.245 262.881); font-weight: bold;',
        'color: oklch(0.147 0.004 49.25); font-weight: normal;',
      ];

      if (event.stack) {
        args.push(event.stack);
      }

      console[event.level].apply(console, args);
    },
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
