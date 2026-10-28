import { act, renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { usePropertyValidation } from '@konfigyr/hooks/vault/property-validation';
import { MessagesProvider } from '@konfigyr/test/helpers/messages';
import type { ReactNode } from 'react';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

import type { ConfigurationPropertyValue, ValidationResult } from '@konfigyr/hooks/vault/types';

interface ValidationCase {
  label: string;
  schema: PropertyJsonSchema;
  value: ConfigurationPropertyValue<unknown> | null;
  keyword?: string;
  message?: string;
}

const propertyValue = (decoded: unknown, encoded = String(decoded)): ConfigurationPropertyValue<unknown> => ({
  encoded,
  decoded,
});

const COMPLEX_OBJECT_SCHEMA: PropertyJsonSchema = {
  type: 'object',
  required: ['username', 'password', 'url'],
  properties: {
    username: {
      type: 'string',
      minLength: 3,
      maxLength: 10,
    },
    password: {
      type: 'string',
      minLength: 8,
      maxLength: 16,
    },
    url: {
      type: 'string',
      minLength: 5,
      maxLength: 30,
    },
  },
};

const VALIDATION_FAIL_CASES: Array<ValidationCase> = [
  {
    label: 'type',
    schema: { type: 'string' },
    value: null,
    keyword: 'type',
    message: 'Must be a valid string',
  }, {
    label: 'enum',
    schema: { type: 'string', enum: ['alpha', 'beta'] },
    value: propertyValue('gamma'),
    keyword: 'enum',
    message: 'Must be one of: alpha, beta',
  }, {
    label: 'type: undecodable number',
    schema: { type: 'integer' },
    value: propertyValue(null, 'not-a-number'),
    keyword: 'type',
    message: 'Must be a valid integer',
  }, {
    label: 'minimum',
    schema: { type: 'number', minimum: 3 },
    value: propertyValue(2),
    keyword: 'minimum',
    message: 'Must be greater than or equal to 3',
  }, {
    label: 'exclusiveMinimum',
    schema: { type: 'number', exclusiveMinimum: 3 },
    value: propertyValue(3),
    keyword: 'exclusiveMinimum',
    message: 'Must be greater than 3',
  }, {
    label: 'maximum',
    schema: { type: 'number', maximum: 3 },
    value: propertyValue(4),
    keyword: 'maximum',
    message: 'Must be less than or equal to 3',
  }, {
    label: 'exclusiveMaximum',
    schema: { type: 'number', exclusiveMaximum: 3 },
    value: propertyValue(3),
    keyword: 'exclusiveMaximum',
    message: 'Must be less than 3',
  }, {
    label: 'multipleOf',
    schema: { type: 'number', multipleOf: 2 },
    value: propertyValue(3),
    keyword: 'multipleOf',
    message: 'Must be a multiple of 2',
  }, {
    label: 'minLength',
    schema: { type: 'string', minLength: 3 },
    value: propertyValue('ab'),
    keyword: 'minLength',
    message: 'Must be at least 3 character(s)',
  }, {
    label: 'maxLength',
    schema: { type: 'string', maxLength: 2 },
    value: propertyValue('abc'),
    keyword: 'maxLength',
    message: 'Must be at most 2 character(s)',
  }, {
    label: 'pattern',
    schema: { type: 'string', pattern: '^abc$' },
    value: propertyValue('abd'),
    keyword: 'pattern',
    message: 'Must match the required format',
  }, {
    label: 'minItems',
    schema: { type: 'array', minItems: 2, items: { type: 'string' } },
    value: propertyValue(['one']),
    keyword: 'minItems',
    message: 'Must contain at least 2 item(s)',
  }, {
    label: 'maxItems',
    schema: { type: 'array', maxItems: 2, items: { type: 'string' } },
    value: propertyValue(['one', 'two', 'three']),
    keyword: 'maxItems',
    message: 'Must contain at most 2 item(s)',
  }, {
    label: 'uniqueItems',
    schema: { type: 'array', uniqueItems: true, items: { type: 'string' } },
    value: propertyValue(['dup', 'dup']),
    keyword: 'uniqueItems',
    message: 'All items must be unique',
  }, {
    label: 'array of formatted strings: duration',
    schema: { type: 'array', items: { type: 'string', format: 'duration' } },
    value: propertyValue([], '1s,5minutes'),
    keyword: 'format',
    message: 'Must be a valid duration',
  }, {
    label: 'array of formatted strings: data-size',
    schema: { type: 'array', items: { type: 'string', format: 'data-size' } },
    value: propertyValue([], '10MB,10MiB'),
    keyword: 'format',
    message: 'Must be a valid data-size',
  }, {
    label: 'array of formatted strings: date-time',
    schema: { type: 'array', items: { type: 'string', format: 'date-time' } },
    value: propertyValue([], 'not-a-datetime'),
    keyword: 'format',
    message: 'Must be a valid date-time',
  }, {
    label: 'array of integers: undecodable item',
    schema: { type: 'array', items: { type: 'integer' } },
    value: propertyValue(null, '1,abc,3'),
    keyword: 'type',
    message: 'Must be a valid array',
  }, {
    label: 'format: date',
    schema: { type: 'string', format: 'date' },
    value: propertyValue('not-a-date'),
    keyword: 'format',
    message: 'Must be a valid date',
  }, {
    label: 'format: time',
    schema: { type: 'string', format: 'time' },
    value: propertyValue('25:61:61'),
    keyword: 'format',
    message: 'Must be a valid time',
  }, {
    label: 'format: date-time',
    schema: { type: 'string', format: 'date-time' },
    value: propertyValue(null, 'not-a-datetime'),
    keyword: 'format',
    message: 'Must be a valid date-time',
  },
  {
    label: 'format: duration string',
    schema: { type: 'string', format: 'duration' },
    value: propertyValue(null, '1hour'),
    keyword: 'format',
    message: 'Must be a valid duration',
  },
  {
    label: 'format: data-size string',
    schema: { type: 'string', format: 'data-size' },
    value: propertyValue(null, '10MiB'),
    keyword: 'format',
    message: 'Must be a valid data-size',
  },
];

const VALIDATION_PASS_CASES: Array<ValidationCase> = [
  { label: 'type', schema: { type: 'string' }, value: propertyValue('abs') },
  { label: 'enum', schema: { type: 'string', enum: ['alpha', 'beta'] }, value: propertyValue('alpha') },
  { label: 'enum relaxed: uppercase', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: propertyValue('ACTIVE') },
  { label: 'enum relaxed: lowercase', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: propertyValue('active') },
  { label: 'enum relaxed: mixed case', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: propertyValue('Active') },
  { label: 'enum relaxed: snake_case', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: propertyValue('in_progress') },
  { label: 'enum relaxed: kebab-case', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: propertyValue('in-progress') },
  { label: 'enum relaxed: camelCase', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: propertyValue('inProgress') },
  { label: 'minimum', schema: { type: 'number', minimum: 3 }, value: propertyValue(5) },
  {
    label: 'array of formatted strings: duration',
    schema: { type: 'array', items: { type: 'string', format: 'duration' } },
    value: propertyValue([], '1s,5m'),
  },
  {
    label: 'array of formatted strings: data-size',
    schema: { type: 'array', items: { type: 'string', format: 'data-size' } },
    value: propertyValue([], '10MB,2GB'),
  },
  {
    label: 'array of formatted strings: date-time',
    schema: { type: 'array', items: { type: 'string', format: 'date-time' } },
    value: propertyValue([], '2024-01-01T00:00:00Z,2025-06-30T12:30:00+02:00'),
  },
  {
    label: 'array of formatted strings: ignores empty segments',
    schema: { type: 'array', items: { type: 'string', format: 'duration' } },
    value: propertyValue([], '1s,,5m,'),
  },
  {
    label: 'array of integers',
    schema: { type: 'array', items: { type: 'integer' } },
    value: propertyValue([0, 8080], '0,8080'),
  },
  {
    label: 'exclusiveMinimum',
    schema: { type: 'number', exclusiveMinimum: 5 },
    value: propertyValue(6),
  },
  { label: 'maximum', schema: { type: 'number', maximum: 3 }, value: propertyValue(3) },
  {
    label: 'exclusiveMaximum',
    schema: { type: 'number', exclusiveMaximum: 3 },
    value: propertyValue(2),
  },
  { label: 'minLength', schema: { type: 'string', minLength: 3 }, value: propertyValue('abc') },
  { label: 'maxLength', schema: { type: 'string', maxLength: 3 }, value: propertyValue('abc') },
  { label: 'pattern', schema: { type: 'string', pattern: '^abc$' }, value: propertyValue('abc') },
  {
    label: 'minItems',
    schema: { type: 'array', minItems: 2, items: { type: 'string' } },
    value: propertyValue(['one', 'two']),
  },
  {
    label: 'maxItems',
    schema: { type: 'array', maxItems: 2, items: { type: 'string' } },
    value: propertyValue(['one', 'two']),
  },
  {
    label: 'uniqueItems',
    schema: { type: 'array', uniqueItems: true, items: { type: 'string' } },
    value: propertyValue(['one', 'two']),
  },
  {
    label: 'format: date',
    schema: { type: 'string', format: 'date' },
    value: propertyValue('2024-02-29'),
  },
  {
    label: 'format: time',
    schema: { type: 'string', format: 'time' },
    value: propertyValue('12:30:45+02:00'),
  },
  {
    label: 'format: date-time',
    schema: { type: 'string', format: 'date-time' },
    value: propertyValue(new Date('2024-01-01T12:30:45.123Z'), '2024-01-01T12:30:45.123Z'),
  },
  {
    label: 'format: duration string',
    schema: { type: 'string', format: 'duration' },
    value: propertyValue({ value: 1, unit: 'h' }, '1h'),
  },
  {
    label: 'format: data-size string',
    schema: { type: 'string', format: 'data-size' },
    value: propertyValue({ value: 10, unit: 'MB' }, '10MB'),
  },
];

describe('hooks | vault | usePropertyValidation', () => {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>{children}</MessagesProvider>
  );

  test.each(VALIDATION_FAIL_CASES)('should fail validation for $label', ({ schema, value, keyword, message }) => {
    const { result } = renderHook(() => usePropertyValidation(schema), { wrapper });

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(value);
    });

    expect(validation.isValid).toBe(false);
    expect(validation.errors.length).toBe(1);

    const [error] = validation.errors;

    expect(error.keyword).toEqual(keyword);
    expect(error.message).toEqual(message);
    expect(result.current.result).toEqual(validation);
  });

  test.each(VALIDATION_PASS_CASES)('should pass validation for $label', ({ schema, value }) => {
    const { result } = renderHook(() => usePropertyValidation(schema), { wrapper });

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(value);
    });

    expect(validation.isValid).toBe(true);
    expect(validation.errors.length).toBe(0);
    expect(result.current.result).toEqual(validation);
  });

  test('should fail validation for complex object', () => {
    const { result } = renderHook(() => usePropertyValidation(COMPLEX_OBJECT_SCHEMA), { wrapper });

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(propertyValue({
        username: 'ab',
        password: 'this-password-is-way-too-long',
      }));
    });

    expect(validation.isValid).toBe(false);
    expect(validation.errors).toHaveLength(3);
    expect(validation.errors).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          keyword: 'minLength',
          message: 'Must be at least 3 character(s)',
        }),
        expect.objectContaining({
          keyword: 'maxLength',
          message: 'Must be at most 16 character(s)',
        }),
        expect.objectContaining({
          keyword: 'required',
          message: 'Missing required property: url',
        }),
      ]),
    );
  });

  test('should passe validation for complex object', () => {
    const { result } = renderHook(() => usePropertyValidation(COMPLEX_OBJECT_SCHEMA), { wrapper });

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(propertyValue({
        username: 'username',
        password: 'valid-password',
        url: 'https://konfugyr.com/login',
      }));
    });

    expect(validation.isValid).toBe(true);
    expect(validation.errors.length).toBe(0);
  });
});
