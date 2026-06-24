import { act, renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { usePropertyValidation } from '@konfigyr/hooks/vault/property-validation';
import { MessagesProvider } from '@konfigyr/test/helpers/messages';
import { DataUnit, DurationUnit } from '@konfigyr/hooks/transforms';
import type { ReactNode } from 'react';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

import type { ValidationResult } from '@konfigyr/hooks/vault/types';

interface ValidationCase {
  label: string;
  schema: PropertyJsonSchema;
  value: unknown;
  keyword?: string;
  format?: string;
  message?: string;
  params?: object;
}

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
    value: 'gamma',
    keyword: 'enum',
    message: 'Must be one of: alpha, beta',
  }, {
    label: 'minimum',
    schema: { type: 'number', minimum: 3 },
    value: 2,
    keyword: 'minimum',
    message: 'Must be greater than or equal to 3',
  }, {
    label: 'exclusiveMinimum',
    schema: { type: 'number', exclusiveMinimum: 3 },
    value: 3,
    keyword: 'exclusiveMinimum',
    message: 'Must be greater than 3',
  }, {
    label: 'maximum',
    schema: { type: 'number', maximum: 3 },
    value: 4,
    keyword: 'maximum',
    message: 'Must be less than or equal to 3',
  }, {
    label: 'exclusiveMaximum',
    schema: { type: 'number', exclusiveMaximum: 3 },
    value: 3,
    keyword: 'exclusiveMaximum',
    message: 'Must be less than 3',
  }, {
    label: 'multipleOf',
    schema: { type: 'number', multipleOf: 2 },
    value: 3,
    keyword: 'multipleOf',
    message: 'Must be a multiple of 2',
  }, {
    label: 'minLength',
    schema: { type: 'string', minLength: 3 },
    value: 'ab',
    keyword: 'minLength',
    message: 'Must be at least 3 character(s)',
  }, {
    label: 'maxLength',
    schema: { type: 'string', maxLength: 2 },
    value: 'abc',
    keyword: 'maxLength',
    message: 'Must be at most 2 character(s)',
  }, {
    label: 'pattern',
    schema: { type: 'string', pattern: '^abc$' },
    value: 'abd',
    keyword: 'pattern',
    message: 'Must match the required format',
  }, {
    label: 'minItems',
    schema: { type: 'array', minItems: 2, items: { type: 'string' } },
    value: ['one'],
    keyword: 'minItems',
    message: 'Must contain at least 2 item(s)',
  }, {
    label: 'maxItems',
    schema: { type: 'array', maxItems: 2, items: { type: 'string' } },
    value: ['one', 'two', 'three'],
    keyword: 'maxItems',
    message: 'Must contain at most 2 item(s)',
  }, {
    label: 'uniqueItems',
    schema: { type: 'array', uniqueItems: true, items: { type: 'string' } },
    value: ['dup', 'dup'],
    keyword: 'uniqueItems',
    message: 'All items must be unique',
  }, {
    label: 'format: date',
    schema: { type: 'string', format: 'date' },
    value: 'not-a-date',
    keyword: 'format',
    format: 'date',
    message: 'Must be a valid date',
  }, {
    label: 'format: time',
    schema: { type: 'string', format: 'time' },
    value: '25:61:61',
    keyword: 'format',
    format: 'time',
    message: 'Must be a valid time',
  }, {
    label: 'format: date-time',
    schema: { type: 'string', format: 'date-time' },
    value: 'not-a-datetime',
    keyword: 'format',
    format: 'date-time',
    message: 'Must be a valid date-time',
  },
  {
    label: 'format: duration string',
    schema: { type: 'string', format: 'duration' },
    value: '1hour',
    format: 'duration',
    keyword: 'format',
    message: 'Must be a valid duration',
  },
  {
    label: 'format: data-size string',
    schema: { type: 'string', format: 'data-size' },
    value: '10MiB',
    format: 'data-size',
    keyword: 'format',
    message: 'Must be a valid data-size',
  },
  {
    label: 'format: Duration object',
    schema: { type: 'object', format: 'duration' },
    value: {
      value: 1,
      unit: 'hour',
    },
    format: 'duration',
    keyword: 'enum',
    message: 'Must be one of: ns, us, ms, s, m, h, d',
    params: {
      allowedValues: Object.values(DurationUnit),
    },
  },
  {
    label: 'format: DataSize object',
    schema: { type: 'object', format: 'data-size' },
    value: {
      value: 1,
      unit: '10MiB',
    },
    format: 'data-size',
    keyword: 'enum',
    message: 'Must be one of: B, KB, MB, GB, TB',
    params: {
      allowedValues: Object.values(DataUnit),
    },
  },
];

const VALIDATION_PASS_CASES: Array<ValidationCase> = [
  { label: 'boolean', schema: { type: 'boolean' }, value: 'true' },
  { label: 'type', schema: { type: 'string' }, value: 'abs' },
  { label: 'enum', schema: { type: 'string', enum: ['alpha', 'beta'] }, value: 'alpha' },
  { label: 'enum relaxed: uppercase', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: 'ACTIVE' },
  { label: 'enum relaxed: lowercase', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: 'active' },
  { label: 'enum relaxed: mixed case', schema: { type: 'string', enum: ['ACTIVE', 'INACTIVE'] }, value: 'Active' },
  { label: 'enum relaxed: snake_case', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: 'in_progress' },
  { label: 'enum relaxed: kebab-case', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: 'in-progress' },
  { label: 'enum relaxed: camelCase', schema: { type: 'string', enum: ['IN_PROGRESS', 'DONE'] }, value: 'inProgress' },
  { label: 'minimum', schema: { type: 'number', minimum: 3 }, value: 5 },
  {
    label: 'exclusiveMinimum',
    schema: { type: 'number', exclusiveMinimum: 5 },
    value: 6,
  },
  { label: 'maximum', schema: { type: 'number', maximum: 3 }, value: 3 },
  {
    label: 'exclusiveMaximum',
    schema: { type: 'number', exclusiveMaximum: 3 },
    value: 2,
  },
  { label: 'minLength', schema: { type: 'string', minLength: 3 }, value: 'abc' },
  { label: 'maxLength', schema: { type: 'string', maxLength: 3 }, value: 'abc' },
  { label: 'pattern', schema: { type: 'string', pattern: '^abc$' }, value: 'abc' },
  {
    label: 'minItems',
    schema: { type: 'array', minItems: 2, items: { type: 'string' } },
    value: ['one', 'two'],
  },
  {
    label: 'maxItems',
    schema: { type: 'array', maxItems: 2, items: { type: 'string' } },
    value: ['one', 'two'],
  },
  {
    label: 'uniqueItems',
    schema: { type: 'array', uniqueItems: true, items: { type: 'string' } },
    value: ['one', 'two'],
  },
  {
    label: 'format: date',
    schema: { type: 'string', format: 'date' },
    value: '2024-02-29',
  },
  {
    label: 'format: time',
    schema: { type: 'string', format: 'time' },
    value: '12:30:45+02:00',
  },
  {
    label: 'format: date-time',
    schema: { type: 'string', format: 'date-time' },
    value: '2024-01-01T12:30:45.123Z',
  },
  {
    label: 'format: duration string',
    schema: { type: 'string', format: 'duration' },
    value: '1h',
  },
  {
    label: 'format: data-size string',
    schema: { type: 'string', format: 'data-size' },
    value: '10MB',
  },
  {
    label: 'format: Duration object',
    schema: { type: 'object', format: 'duration' },
    value: {
      value: 1,
      unit: 'h',
    },
  },
  {
    label: 'format: DataSize object',
    schema: { type: 'object', format: 'data-size' },
    value: {
      value: 1,
      unit: 'MB',
    },
  },
];

describe('hooks | vault | usePropertyValidation', () => {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>{children}</MessagesProvider>
  );

  test.each(VALIDATION_FAIL_CASES)('should fail validation for $label', ({ schema, value, keyword, format, message, params }) => {
    const { result } = renderHook(() => usePropertyValidation(schema), { wrapper });

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(value);
    });

    expect(validation.isValid).toBe(false);
    expect(validation.errors.length).toBe(1);
    expect(validation.errors[0]).toMatchObject({
      keyword,
      message,
      ...(format ? { params: { ...params } } : {}),
    });
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
      validation = result.current.validate({
        username: 'ab',
        password: 'this-password-is-way-too-long',
      });
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
      validation = result.current.validate({
        username: 'username',
        password: 'valid-password',
        url: 'https://konfugyr.com/login',
      });
    });

    expect(validation.isValid).toBe(true);
    expect(validation.errors.length).toBe(0);
  });
});
