import { useCallback, useMemo, useState } from 'react';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import { useIntl } from 'react-intl';
import { VALIDATION_MESSAGES } from '@konfigyr/hooks/vault/messages';
import { DATA_SIZE_ENCODED_PATTERN, DURATION_ENCODED_PATTERN } from '@konfigyr/hooks/transforms';
import type { ErrorObject, ValidateFunction } from 'ajv';
import type { IntlShape } from 'react-intl';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { ConfigurationPropertyValue, ValidationError, ValidationResult } from '@konfigyr/hooks/vault/types';

type ValidatedValue = ConfigurationPropertyValue<unknown> | undefined | null;

const ajv = new Ajv({ allErrors: true, coerceTypes: false });
addFormats(ajv);
ajv.addFormat('duration', DURATION_ENCODED_PATTERN);
ajv.addFormat('data-size', DATA_SIZE_ENCODED_PATTERN);

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

function createPropertyValidator(schema: PropertyJsonSchema): ValidateFunction {
  return ajv.compile(schema);
}

function validatePropertyValue(
  validator: ValidateFunction,
  schema: PropertyJsonSchema,
  value?: ValidatedValue,
): boolean {
  let subject: unknown = value?.decoded;

  // when value type is string, use the encoded string representation for
  // validation, AJV uses different patterns to validate custom schema formats
  if (schema.type === 'string') {
    subject = value?.encoded;
  }

  return schema.enum?.length
    ? validator(resolveRelaxedEnumValue(schema.enum, subject))
    : validator(subject);
}

/**
 * Checks if the configuration property value satisfies the given JSON schema.
 *
 * Values of `string` schemas are validated using their encoded representation. This allows
 * custom string formats, like `duration` or `data-size`, to be checked against their encoded
 * patterns. Values of all other schema types are validated using their decoded representation.
 *
 * String values of `enum` schemas are matched using Spring Boot relaxed binding rules.
 *
 * @param schema the JSON schema of the configuration property
 * @param value the configuration property value to validate, can be `undefined`
 * @returns `true` when the value is valid, `false` otherwise
 */
export function isPropertyValueValid(schema: PropertyJsonSchema, value: ValidatedValue): boolean {
  const validator = createPropertyValidator(schema);
  return validatePropertyValue(validator, schema, value);
}

/**
 * Hook that validates configuration property values against the given JSON schema.
 *
 * Values are validated using the same rules as {@link isPropertyValueValid}. Failed
 * validations contain localized error messages for each violated schema keyword.
 *
 * The result of the latest validation is kept in state. It is valid until the first
 * `validate` call.
 *
 * @param schema the JSON schema of the configuration property
 * @returns the `validate` function and the result of the latest validation
 */
export function usePropertyValidation(schema: PropertyJsonSchema): {
  validate: (value: ValidatedValue) => ValidationResult;
  result: ValidationResult;
} {
  const intl: IntlShape = useIntl();
  const validator = useMemo<ValidateFunction>(() => createPropertyValidator(schema), [schema]);

  const [result, setResult] = useState<ValidationResult>(() => buildValidationResult(true, []));

  const validate = useCallback((value: ValidatedValue): ValidationResult => {
    const isValid = validatePropertyValue(validator, schema, value);

    const nextResult: ValidationResult = isValid
      ? buildValidationResult(true, [])
      : buildValidationResult(false, mapAjvErrors(validator.errors ?? [], intl));

    setResult(nextResult);
    return nextResult;
  }, [intl, validator]);

  return { validate, result };
}
