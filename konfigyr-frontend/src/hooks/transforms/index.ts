/**
 * Interface that defines the structure of a Transform that would be used to transform
 * strings into a more convenient format.
 */
export interface Transform<T> {
  /**
   * Encodes the given value into a string representation.
   *
   * @param value the value to be encoded
   * @returns the encoded value
   */
  encode: (value: T) => string;

  /**
   * Decodes the given string value into the original value.
   *
   * @param value the value to be decoded
   * @returns the decoded value or null if the value could not be decoded
   */
  decode: (value: string) => T | null;
}

/**
 * Default Transform that would be used for all string values.
 */
export const stringTransform: Transform<string> = {
  encode: (value) => value,
  decode: (value) => value,
};

/**
 * Transform that would be used for boolean values.
 */
export const booleanTransform: Transform<boolean> = {
  encode: (value) => value ? 'true' : 'false',
  decode: (value) => /^(true|t|1)$/i.test(value),
};

/**
 * Transform that would be used for number values.
 */
export const numberTransform: Transform<number> = {
  encode: (value) => String(value),
  decode: (value) => {
    const decoded = Number(value);
    return isNaN(decoded) ? null : decoded;
  },
};

/**
 * Transform that would be used for Date values.
 */
export const dateTransform: Transform<Date> = {
  encode: (value) => value.toISOString(),
  decode: (value) => new Date(value),
};

/**
 * Basic interface that represents a duration value. The type consists out of the
 * value and the time unit, for example, `6 hours`. Keep in mind that the unit
 * is singular, for example, `hour` instead of `hours`.
 */
export enum DurationUnit {
  /**
   * Nanoseconds.
   */
  NANOSECONDS = 'ns',

  /**
   * Microseconds.
   */
  MICROSECONDS = 'us',

  /**
   * Milliseconds.
   */
  MILLISECONDS = 'ms',

  /**
   * Seconds.
   */
  SECONDS = 's',

  /**
   * Minutes.
   */
  MINUTES = 'm',

  /**
   * Hours.
   */
  HOURS = 'h',

  /**
   * Days.
   */
  DAYS = 'd',
}

/**
 * Basic interface that represents a duration value. The type consists out of the
 * value and the time unit, for example, `6 hours`. Keep in mind that the unit
 * is singular, for example, `hour` instead of `hours`.
 */
export interface Duration {
  value: number;
  unit: DurationUnit;
}

/**
 * Transform that would be used for Duration values.
 */
export const durationTransform: Transform<Duration> = {
  encode: (duration) => `${duration.value}${duration.unit}`,
  decode: (value) => {
    const match = value.match(/^([+-]?\d+)([a-zA-Z]{0,2})$/);

    if (match && Object.values(DurationUnit).includes(match[2] as DurationUnit)) {
      const duration = numberTransform.decode(match[1]);

      if (duration === null) {
        return null;
      }

      return { value: duration, unit: match[2] as DurationUnit };
    }

    return null;
  },
};

export enum DataUnit {
  /**
   * Bytes, represented by suffix B.
   */
  BYTES = 'B',

  /**
   * Kilobytes, represented by suffix {@code KB}.
   */
  KILOBYTES = 'KB',

  /**
   * Megabytes, represented by suffix {@code MB}.
   */
  MEGABYTES = 'MB',

  /**
   * Gigabytes, represented by suffix {@code GB}.
   */
  GIGABYTES = 'GB',

  /**
   * Terabytes, represented by suffix {@code TB}.
   */
  TERABYTES = 'TB',
}

/**
 * Interface that represents a data size. The type consists out of the value and the data unit,
 * for example, `256 Gigabytes`.
 *
 */
export interface DataSize {
  value: number;
  unit: DataUnit;
}

/**
 * Transform that would be used for DataSize values.
 */
export const dataSizeTransform: Transform<DataSize> = {
  encode: (size) => `${size.value}${size.unit}`,
  decode: (value) => {
    const match = value.match(/^([+-]?\d+)([a-zA-Z]{0,2})$/);

    if (match && Object.values(DataUnit).includes(match[2] as DataUnit)) {
      const size = numberTransform.decode(match[1]);

      if (size === null) {
        return null;
      }

      return { value: size, unit: match[2] as DataUnit };
    }

    return null;
  },
};
