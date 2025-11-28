import { afterAll, describe, expect, test, vi } from 'vitest';
import { createLogger } from '@konfigyr/logger';
import { LogLevel, printLoggerEvent, resolveLoggerLevel } from '@konfigyr/logger/browser';

describe('middleware | client', () => {
  afterAll(() => vi.restoreAllMocks());

  test('should create child logger', () => {
    const logger = createLogger('test');

    expect(logger).toBeDefined();
  });

  test('should resolve logger label to level', () => {
    expect(resolveLoggerLevel('trace')).toStrictEqual({ level: LogLevel.DEBUG });
    expect(resolveLoggerLevel('debug')).toStrictEqual({ level: LogLevel.DEBUG });
    expect(resolveLoggerLevel('info')).toStrictEqual({ level: LogLevel.INFO });
    expect(resolveLoggerLevel('warning')).toStrictEqual({ level: LogLevel.WARN });
    expect(resolveLoggerLevel('error')).toStrictEqual({ level: LogLevel.ERROR });
    expect(resolveLoggerLevel('unknown')).toStrictEqual({ level: LogLevel.DEBUG });
  });

  test('should print logging event to console', () => {
    const logger = vi.spyOn(console, 'info').mockImplementation(() => undefined);

    printLoggerEvent({
      time: 1764328269198,
      name: 'test-logger',
      level: LogLevel.INFO,
      msg: 'test message',
    });

    expect(logger).toHaveBeenCalledWith(
      '%c12:11:09 PM %cINFO %c[test-logger] %ctest message',
      'color: oklch(0.373 0.034 259.733);',
      'color: oklch(0.216 0.006 56.043); font-weight: bold;',
      'color: oklch(0.546 0.245 262.881); font-weight: bold;',
      'color: oklch(0.147 0.004 49.25); font-weight: normal;',
    );
  });

  test('should print logging event with an error to console', () => {
    const logger = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    const error = new Error('Ooops');

    printLoggerEvent({
      time: 1764328269198,
      name: 'test-logger',
      level: LogLevel.WARN,
      msg: 'test message',
      stack: error.stack,
    });

    expect(logger).toHaveBeenCalledWith(
      '%c12:11:09 PM %cWARN %c[test-logger] %ctest message',
      'color: oklch(0.373 0.034 259.733);',
      'color: oklch(0.216 0.006 56.043); font-weight: bold;',
      'color: oklch(0.546 0.245 262.881); font-weight: bold;',
      'color: oklch(0.147 0.004 49.25); font-weight: normal;',
      error.stack,
    );
  });

});
