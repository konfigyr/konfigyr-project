import { useCallback, useMemo, useState } from 'react';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import { useIntl } from 'react-intl';
import { VALIDATION_MESSAGES } from '@konfigyr/hooks/vault/messages';
import { DATA_SIZE_ENCODED_PATTERN, DURATION_ENCODED_PATTERN, DataUnit, DurationUnit } from '@konfigyr/hooks/transforms';
import type { ErrorObject, ValidateFunction } from 'ajv';
import type { IntlShape } from 'react-intl';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { ValidationError, ValidationResult } from '@konfigyr/hooks/vault/types';

const ajv = new Ajv({ allErrors: true, coerceTypes: false });
addFormats(ajv);

const [ DURATION_FORMAT, DATA_SIZE_FORMAT ] = ['duration', 'data-size'];
const BOOLEAN_ENCODED_PATTERN = /^(true|1|false|0)$/i;
const BOOLEAN_TRUE_PATTERN = /^(true|1)$/i;

ajv.addFormat(DURATION_FORMAT, DURATION_ENCODED_PATTERN);
ajv.addFormat(DATA_SIZE_FORMAT, DATA_SIZE_ENCODED_PATTERN);

/**
 * Builds a JSON Schema for unit-based scalar objects.
 *
 * The schema validates a shape with:
 * - `value`: numeric amount
 * - `unit`: one of the supplied unit values
 */
const buildUnitSchema = (units: Array<string>): object => ({
  type: 'object',
  properties: {
    value: {
      type: 'number',
    },
    unit: {
      type: 'string',
      enum: units,
    },
  },
  required: ['value', 'unit'],
  additionalProperties: false,
});

/**
 * JSON Schema that validates a duration object consisting of:
 * - `value`: the duration amount
 * - `unit`: one of the supported {@link DurationUnit} values
 *
 * @see Duration
 */
const DURATION_SCHEMA = buildUnitSchema(Object.values(DurationUnit));

/**
 * JSON Schema that validates a data size object consisting of:
 * - `value`: the data size amount
 * - `unit`: one of the supported {@link DataUnit} values
 *
 * @see DataSize
 */
const DATA_SIZE_SCHEMA = buildUnitSchema(Object.values(DataUnit));

// Normalizes enum values to support Spring Boot relaxed binding.
// Converts to uppercase and removes separators (-, _, whitespace) so values
// like ACTIVE, active, in-progress, in_progress, and inProgress
// are treated as equivalent.
const NORMALIZE_ENUM_PATTERN = /[-_\s]/g;
const normalizeEnumValue = (value: string): string => value.replace(NORMALIZE_ENUM_PATTERN, '').toUpperCase();

const resolveRelaxedEnumValue = (enumValues: Array<string>, value: unknown): unknown => {
  if (typeof value !== 'string') {
    return value;
  }
  const normalized = normalizeEnumValue(value);
  return enumValues.find(it => normalizeEnumValue(it) === normalized) ?? value;
};

const toValueList = (params: Record<string, any>): string => {
  const values = params.allowedValues ?? [];

  if (!Array.isArray(values)) {
    return '';
  }
  return values.map(value => String(value)).join(', ');
};

const toMissingProperty = (params: Record<string, any>): string => String((params as { missingProperty?: string }).missingProperty);

const toLimit = (params: Record<string, any>): string => String((params as { limit?: number }).limit);

const toFormat = (params: Record<string, any>): string => String((params as { format?: number }).format);

const toMultipleOf = (params: Record<string, any>): string => String((params as { multipleOf?: number }).multipleOf);

const mapAjvError = (error: ErrorObject, intl: IntlShape): ValidationError => {
  const { keyword, params } = error;

  switch (keyword) {
    case 'type':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.type, { type: String(params.type) }),
      };
    case 'enum':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.enum, { values: toValueList(params) }),
      };
    case 'required':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.required, { property: toMissingProperty(params) }),
      };
    case 'minimum':
    case 'exclusiveMinimum':
    case 'maximum':
    case 'exclusiveMaximum':
    case 'minLength':
    case 'maxLength':
    case 'minItems':
    case 'maxItems':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES[keyword], { limit: toLimit(params) }),
      };
    case 'multipleOf':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.multipleOf, { value: toMultipleOf(params) }),
      };
    case 'pattern':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.pattern),
      };
    case 'format':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.format, { format: toFormat(params) }),
      };
    case 'uniqueItems':
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.uniqueItems),
      };
    default:
      return {
        keyword,
        params,
        message: intl.formatMessage(VALIDATION_MESSAGES.invalid),
      };
  }
};

const mapAjvErrors = (errors: Array<ErrorObject>, intl: IntlShape): Array<ValidationError> => errors.map(error => mapAjvError(error, intl));

const buildValidationResult = (isValid: boolean, errors: Array<ValidationError>) => ({ isValid, errors });

function createPropertyValidator (schema: PropertyJsonSchema): ValidateFunction {
  if (schema.type === 'string' && (schema.format === DURATION_FORMAT || schema.format === DATA_SIZE_FORMAT)) {
    return ajv.compile(schema);
  }

  if (schema.type === 'object' && schema.format === DURATION_FORMAT) {
    return ajv.compile(DURATION_SCHEMA);
  }

  if (schema.type === 'object' && schema.format === DATA_SIZE_FORMAT) {
    return ajv.compile(DATA_SIZE_SCHEMA);
  }

  return ajv.compile(schema);
}

/**
 * Normalizes an encoded property value before schema validation.
 *
 * Currently, this only coerces boolean-like strings into actual booleans so
 * the validator can accept values such as `"true"` and `"false"`.
 */
function normalizePropertyValue(schema: PropertyJsonSchema, value: unknown): unknown {
  if (value === null || value === undefined || typeof value !== 'string') {
    return value;
  }

  if (schema.type === 'boolean') {
    if (!BOOLEAN_ENCODED_PATTERN.test(value)) {
      return value;
    }
    return BOOLEAN_TRUE_PATTERN.test(value);
  }

  return value;
}

function validatePropertyValue(validator: ValidateFunction, schema: PropertyJsonSchema, value: unknown): boolean {
  const normalizedValue = normalizePropertyValue(schema, value);

  return schema.enum?.length
    ? validator(resolveRelaxedEnumValue(schema.enum, normalizedValue))
    : validator(normalizedValue);
}

export function isPropertyValueValid(schema: PropertyJsonSchema, value: unknown): boolean {
  const validator = createPropertyValidator(schema);
  return validatePropertyValue(validator, schema, value);
}

export function usePropertyValidation (schema: PropertyJsonSchema): {
  validate: (value: unknown) => ValidationResult;
  result: ValidationResult;
} {
  const intl: IntlShape = useIntl();
  const validator = useMemo<ValidateFunction>(() => createPropertyValidator(schema), [schema]);

  const [result, setResult] = useState<ValidationResult>(() => buildValidationResult(true, []));

  const validate = useCallback((value: unknown): ValidationResult => {
    const isValid = validatePropertyValue(validator, schema, value);

    const nextResult: ValidationResult = isValid
      ? buildValidationResult(true, [])
      : buildValidationResult(false, mapAjvErrors(validator.errors ?? [], intl));

    setResult(nextResult);
    return nextResult;
  }, [validator]);

  return {
    validate,
    result,
  };
}
