import { describe, expect, test } from 'vitest';
import { resolveJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';

describe('hooks | artifactory | resolveJsonSchemeTransform', () => {
  test('should transform array of strings', () => {
    const transform = resolveJsonSchemeTransform<Array<string>>({ type: 'array', items: { type: 'string' } });

    expect(transform.decode('foo,bar,baz')).toStrictEqual(['foo', 'bar', 'baz']);
    expect(transform.decode('foo,,bar')).toStrictEqual(['foo', 'bar']);
    expect(transform.decode('')).toStrictEqual([]);
    expect(transform.encode(['foo', 'bar', 'baz'])).toBe('foo,bar,baz');
  });

  test('should keep falsy numeric values when transforming array of integers', () => {
    const transform = resolveJsonSchemeTransform<Array<number>>({ type: 'array', items: { type: 'integer' } });

    expect(transform.decode('0,8080')).toStrictEqual([0, 8080]);
    expect(transform.decode('0,invalid,8080')).toStrictEqual([0, 8080]);
    expect(transform.decode('8080,')).toStrictEqual([8080]);
  });

  test('should keep falsy boolean values when transforming array of booleans', () => {
    const transform = resolveJsonSchemeTransform<Array<boolean>>({ type: 'array', items: { type: 'boolean' } });

    expect(transform.decode('true,false')).toStrictEqual([true, false]);
  });
});
