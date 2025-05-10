import { describe, expect, test } from 'vitest';
import { Locale, resolve } from 'konfigyr/i18n';

describe('i18n', () => {
  test('should resolve i18n-next config', async () => {
    const config = resolve(Locale.ENGLISH);

    expect(config.locale).toBe(Locale.ENGLISH);
    expect(config.messages).toBeInstanceOf(Object);
  });

  test('should resolve i18n-next config for unknown locale', async () => {
    expect(() => resolve('unknown' as Locale)).toThrowError('Unsupported locale of: \'unknown\'.');
  });
});
