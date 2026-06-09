import { act, renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';

import { usePropertyValidation } from '@konfigyr/hooks/vault/property-validation';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

import type { ValidationResult } from '@konfigyr/hooks/vault/types';

interface ValidationCase {
  label: string;
  schema: PropertyJsonSchema;
  value: unknown;
  keyword?: string;
  format?: string;
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
  { label: 'type', schema: { type: 'string' }, value: null, keyword: 'type' },
  { label: 'enum', schema: { type: 'string', enum: ['alpha', 'beta'] }, value: 'gamma', keyword: 'enum' },
  { label: 'minimum', schema: { type: 'number', minimum: 3 }, value: 2, keyword: 'minimum' },
  {
    label: 'exclusiveMinimum',
    schema: { type: 'number', exclusiveMinimum: 3 },
    value: 3,
    keyword: 'exclusiveMinimum',
  },
  { label: 'maximum', schema: { type: 'number', maximum: 3 }, value: 4, keyword: 'maximum' },
  {
    label: 'exclusiveMaximum',
    schema: { type: 'number', exclusiveMaximum: 3 },
    value: 3,
    keyword: 'exclusiveMaximum',
  },
  { label: 'multipleOf', schema: { type: 'number', multipleOf: 2 }, value: 3, keyword: 'multipleOf' },
  { label: 'minLength', schema: { type: 'string', minLength: 3 }, value: 'ab', keyword: 'minLength' },
  { label: 'maxLength', schema: { type: 'string', maxLength: 2 }, value: 'abc', keyword: 'maxLength' },
  { label: 'pattern', schema: { type: 'string', pattern: '^abc$' }, value: 'abd', keyword: 'pattern' },
  {
    label: 'minItems',
    schema: { type: 'array', minItems: 2, items: { type: 'string' } },
    value: ['one'],
    keyword: 'minItems',
  },
  {
    label: 'maxItems',
    schema: { type: 'array', maxItems: 2, items: { type: 'string' } },
    value: ['one', 'two', 'three'],
    keyword: 'maxItems',
  },
  {
    label: 'uniqueItems',
    schema: { type: 'array', uniqueItems: true, items: { type: 'string' } },
    value: ['dup', 'dup'],
    keyword: 'uniqueItems',
  },
  {
    label: 'format: date',
    schema: { type: 'string', format: 'date' },
    value: 'not-a-date',
    keyword: 'format',
    format: 'date',
  },
  {
    label: 'format: time',
    schema: { type: 'string', format: 'time' },
    value: '25:61:61',
    keyword: 'format',
    format: 'time',
  },
  {
    label: 'format: date-time',
    schema: { type: 'string', format: 'date-time' },
    value: 'not-a-datetime',
    keyword: 'format',
    format: 'date-time',
  },
  {
    label: 'format: duration',
    schema: { type: 'string', format: 'duration' },
    value: '1h30m',
    keyword: 'format',
    format: 'duration',
  },
  {
    label: 'format: data-size',
    schema: { type: 'string', format: 'data-size' },
    value: '10MiB',
    keyword: 'format',
    format: 'data-size',
  },
];

const VALIDATION_PASS_CASES: Array<ValidationCase> = [
  { label: 'type', schema: { type: 'string' }, value: 'abs' },
  { label: 'enum', schema: { type: 'string', enum: ['alpha', 'beta'] }, value: 'alpha' },
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
    label: 'format: duration',
    schema: { type: 'string', format: 'duration' },
    value: '1h',
  },
  {
    label: 'format: data-size',
    schema: { type: 'string', format: 'data-size' },
    value: '10MB',
  },
];

describe('hooks | vault | usePropertyValidation', () => {

  test.each(VALIDATION_FAIL_CASES)('should fail validation for $label', ({ schema, value, keyword, format }) => {
    const { result } = renderHook(() => usePropertyValidation(schema));

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(value);
    });

    expect(validation.isValid).toBe(false);
    expect(validation.errors.length).toBe(1);
    expect(validation.errors[0]).toMatchObject({
      keyword,
      message: expect.any(String),
      ...(format ? { params: { format } } : {}),
    });
    expect(result.current.result).toEqual(validation);
  });

  test.each(VALIDATION_PASS_CASES)('should pass validation for $label', ({ schema, value }) => {
    const { result } = renderHook(() => usePropertyValidation(schema));

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate(value);
    });

    expect(validation.isValid).toBe(true);
    expect(validation.errors.length).toBe(0);
    expect(result.current.result).toEqual(validation);
  });

  test('should fail validation for complex object', () => {

    const { result } = renderHook(() => usePropertyValidation(COMPLEX_OBJECT_SCHEMA));

    let validation!: ValidationResult;

    act(() => {
      validation = result.current.validate({
        username: 'ab',
        password: 'this-password-is-way-too-long',
        url: 12345,
      });
    });

    expect(validation.isValid).toBe(false);
    expect(validation.errors.length).toBe(3);

    expect(validation.errors[0], 'username validation').toMatchObject({
      keyword: 'minLength',
      message: expect.any(String),
    });

    expect(validation.errors[1], 'password validation').toMatchObject({
      keyword: 'maxLength',
      message: expect.any(String),
    });

    expect(validation.errors[2], 'url validation').toMatchObject({
      keyword: 'type',
      message: expect.any(String),
    });
  });

  test('should passe validation for complex object', () => {

    const { result } = renderHook(() => usePropertyValidation(COMPLEX_OBJECT_SCHEMA));

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
