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

export function usePropertyValidation (schema: PropertyJsonSchema): {
  validate: (value: unknown) => ValidationResult;
  result: ValidationResult;
} {
  const intl: IntlShape = useIntl();
  const validator = useMemo<ValidateFunction<unknown>>(() => ajv.compile(schema), [schema]);

  const [result, setResult] = useState<ValidationResult>(() => buildValidationResult(true, []));

  const validate = useCallback((value: unknown): ValidationResult => {
    const isValid = validator(value);

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
