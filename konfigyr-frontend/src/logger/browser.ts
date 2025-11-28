export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error',
}

export interface LoggerEvent {
  name: string,
  time: number,
  level: LogLevel,
  msg: string,
  stack?: string,
}

/**
 * Resolves the browser logger level based on the given logger event label.
 *
 * @param label logger event label
 */
export function resolveLoggerLevel(label: string | undefined | null = 'debug'):  { level: LogLevel } {
  switch (label) {
    case 'info':
      return { level: LogLevel.INFO };
    case 'warn':
    case 'warning':
      return { level: LogLevel.WARN };
    case 'error':
      return { level: LogLevel.ERROR };
    default:
      return { level: LogLevel.DEBUG };
  }
}

/**
 * Prints the given logger event to the browser console.
 *
 * @param event the logger event to be printed
 */
export function printLoggerEvent(event: LoggerEvent): void {
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

  // @ts-ignore: we are invoking a native console method, TypeScript doesn't know about it
  console[event.level].apply(console, args);
}
