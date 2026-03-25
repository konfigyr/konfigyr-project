import { describe, expect, test } from 'vitest';
import {
  DataUnit,
  DurationUnit,
  booleanTransform,
  dataSizeTransform,
  dateTransform,
  durationTransform,
  numberTransform,
  stringTransform,
} from '@konfigyr/hooks/transforms';

describe('hooks | transforms', () => {
  test('should transform string type', () => {
    expect(stringTransform.encode('to encode')).toBe('to encode');
    expect(stringTransform.decode('to decode')).toBe('to decode');
  });

  test('should transform numeric type', () => {
    expect(numberTransform.encode(12345)).toBe('12345');
    expect(numberTransform.encode(123.004500)).toBe('123.0045');

    expect(numberTransform.decode('512635')).toBe(512635);
    expect(numberTransform.decode('0.63050')).toBe(0.6305);
    expect(numberTransform.decode('.125')).toBe(0.125);
    expect(numberTransform.decode('invalid')).toBeNull();
  });

  test('should transform boolean type', () => {
    expect(booleanTransform.encode(true)).toBe('true');
    expect(booleanTransform.encode(false)).toBe('false');

    expect(booleanTransform.decode('1')).toBe(true);
    expect(booleanTransform.decode('t')).toBe(true);
    expect(booleanTransform.decode('true')).toBe(true);
    expect(booleanTransform.decode('false')).toBe(false);
    expect(booleanTransform.decode('something else')).toBe(false);
  });

  test('should transform date type', () => {
    const date = new Date('2024-01-01T00:00:00.000Z');
    expect(dateTransform.encode(date)).toBe(date.toISOString());

    expect(dateTransform.decode(date.toISOString())).toStrictEqual(date);
    expect(dateTransform.decode('invalid date string')).toBeNull();
  });

  test('should transform data size type', () => {
    expect(dataSizeTransform.encode({ value: 135, unit: DataUnit.GIGABYTES })).toBe('135GB');

    expect(dataSizeTransform.decode('1352B')).toStrictEqual({ value: 1352, unit: DataUnit.BYTES });
    expect(dataSizeTransform.decode('5712KB')).toStrictEqual({ value: 5712, unit: DataUnit.KILOBYTES });
    expect(dataSizeTransform.decode('8MB')).toStrictEqual({ value: 8, unit: DataUnit.MEGABYTES });
    expect(dataSizeTransform.decode('016GB')).toStrictEqual({ value: 16, unit: DataUnit.GIGABYTES });
    expect(dataSizeTransform.decode('1024TB')).toStrictEqual({ value: 1024, unit: DataUnit.TERABYTES });

    expect(dataSizeTransform.decode('11')).toBeNull();
    expect(dataSizeTransform.decode('11M')).toBeNull();
    expect(dataSizeTransform.decode('11mm')).toBeNull();
    expect(dataSizeTransform.decode('GB')).toBeNull();
    expect(dataSizeTransform.decode('1.11GB')).toBeNull();
    expect(dataSizeTransform.decode('1,11TB')).toBeNull();
  });

  test('should transform duration type', () => {
    expect(durationTransform.encode({ value: 1346, unit: DurationUnit.HOURS })).toBe('1346h');

    expect(durationTransform.decode('011ns')).toStrictEqual({ value: 11, unit: DurationUnit.NANOSECONDS });
    expect(durationTransform.decode('10us')).toStrictEqual({ value: 10, unit: DurationUnit.MICROSECONDS });
    expect(durationTransform.decode('356ms')).toStrictEqual({ value: 356, unit: DurationUnit.MILLISECONDS });
    expect(durationTransform.decode('1234s')).toStrictEqual({ value: 1234, unit: DurationUnit.SECONDS });
    expect(durationTransform.decode('1251235m')).toStrictEqual({ value: 1251235, unit: DurationUnit.MINUTES });
    expect(durationTransform.decode('24h')).toStrictEqual({ value: 24, unit: DurationUnit.HOURS });
    expect(durationTransform.decode('77d')).toStrictEqual({ value: 77, unit: DurationUnit.DAYS });

    expect(durationTransform.decode('11')).toBeNull();
    expect(durationTransform.decode('11M')).toBeNull();
    expect(durationTransform.decode('11mm')).toBeNull();
    expect(durationTransform.decode('ms')).toBeNull();
    expect(durationTransform.decode('1.11h')).toBeNull();
    expect(durationTransform.decode('1,11h')).toBeNull();
  });
});
