import { useCallback, useMemo, useState } from 'react';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import { useIntl } from 'react-intl';
import { VALIDATION_MESSAGES } from '@konfigyr/hooks/vault/messages';
import type { ErrorObject, ValidateFunction } from 'ajv';
import type { IntlShape } from 'react-intl';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { ValidationError, ValidationResult } from '@konfigyr/hooks/vault/types';

const ajv = new Ajv({ allErrors: true, coerceTypes: false });

addFormats(ajv);

ajv.addFormat('duration', /^[+-]?\d+(ns|us|ms|s|m|h|d)$/);
ajv.addFormat('data-size', /^[+-]?\d+(B|KB|MB|GB|TB)$/);

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

function getDefaultValidator (schema: PropertyJsonSchema): ValidateFunction {
  return ajv.compile(schema);
}

function validatePropertyValue(validator: ValidateFunction, schema: PropertyJsonSchema, value: unknown): boolean {
  return schema.enum?.length
    ? validator(resolveRelaxedEnumValue(schema.enum, value))
    : validator(value);
}

export function isPropertyValueValid(schema: PropertyJsonSchema, value: unknown): boolean {
  const validator = getDefaultValidator(schema);
  return validatePropertyValue(validator, schema, value);
}

export function usePropertyValidation (schema: PropertyJsonSchema): {
  validate: (value: unknown) => ValidationResult;
  result: ValidationResult;
} {
  const intl: IntlShape = useIntl();
  const validator = useMemo<ValidateFunction>(() => getDefaultValidator(schema), [schema]);

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
