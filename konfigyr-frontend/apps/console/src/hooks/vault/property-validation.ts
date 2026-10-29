import { useCallback, useMemo, useState } from 'react';
import Schema from 'z-schema';
import { useIntl } from 'react-intl';
import { VALIDATION_MESSAGES } from '@konfigyr/hooks/vault/messages';
import { DATA_SIZE_ENCODED_PATTERN, DURATION_ENCODED_PATTERN } from '@konfigyr/hooks/transforms';

import type { IntlShape } from 'react-intl';
import type { FormatValidatorFn, SchemaErrorDetail, ValidateOptions, ValidateResponse } from 'z-schema';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { ConfigurationPropertyValue, ValidationError, ValidationResult } from '@konfigyr/hooks/vault/types';

type ValidatedValue = ConfigurationPropertyValue<unknown> | undefined | null;
type ValidateFunction = (json: unknown, options?: ValidateOptions) => ValidateResponse;

const schemaValidator = Schema.create({ safe: true });

function patternFormatFactory(pattern: RegExp): FormatValidatorFn {
  return (input) => {
    if (typeof input === 'string') {
      return pattern.test(input);
    }
    return false;
  };
}

Schema.registerFormat('duration', patternFormatFactory(DURATION_ENCODED_PATTERN));
Schema.registerFormat('data-size', patternFormatFactory(DATA_SIZE_ENCODED_PATTERN));

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

const toValueList = (values?: unknown): string => {
  if (!Array.isArray(values)) {
    return '';
  }
  return values.map(value => String(value)).join(', ');
};

const mapAjvError = (error: SchemaErrorDetail, schema: PropertyJsonSchema, intl: IntlShape): ValidationError => {
  const { keyword } = error;

  switch (keyword) {
    case 'type':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES.type, {
          type: String(error.params[0]),
          value: String(error.params[1]),
        }),
      };
    case 'enum':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES.enum, {
          value: String(error.params[0]),
          values: toValueList(schema.enum),
        }),
      };
    case 'required':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES.required, { property: toValueList(error.params) }),
      };
    case 'minimum':
    case 'exclusiveMinimum':
    case 'maximum':
    case 'exclusiveMaximum':
    case 'minLength':
    case 'maxLength':
    case 'minItems':
    case 'maxItems':
    case 'multipleOf':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES[keyword], {
          value: String(error.params[0]),
          limit: String(error.params[1]),
        }),
      };
    case 'pattern':
      return { keyword, message: intl.formatMessage(VALIDATION_MESSAGES.pattern) };
    case 'format':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES.format, {
          format: String(error.params[0]),
          value: String(error.params[1]),
        }),
      };
    case 'uniqueItems':
      return {
        keyword,
        message: intl.formatMessage(VALIDATION_MESSAGES.uniqueItems),
      };
    default:
      return {
        keyword: String(keyword),
        message: intl.formatMessage(VALIDATION_MESSAGES.invalid),
      };
  }
};

const mapAjvErrors = (errors: Array<SchemaErrorDetail>, schema: PropertyJsonSchema, intl: IntlShape): Array<ValidationError> => errors.map(error => mapAjvError(error, schema, intl));

const buildValidationResult = (isValid: boolean, errors: Array<ValidationError>) => ({ isValid, errors });

function createPropertyValidator(schema: PropertyJsonSchema): ValidateFunction {
  return schemaValidator.compile(schema);
}

// Resolves the value that should be validated against the schema. The encoded string
// representation is used for `string` schemas and for arrays of strings, where it is
// split into the encoded items. All other schemas use the decoded value.
function resolveValidationSubject(schema: PropertyJsonSchema, value?: ValidatedValue): unknown {
  if (schema.type === 'string') {
    return value?.encoded;
  }

  if (schema.type === 'array' && schema.items?.type === 'string') {
    return value?.encoded.split(',').filter(item => item !== '');
  }

  return value?.decoded;
}

function validatePropertyValue(
  validator: ValidateFunction,
  schema: PropertyJsonSchema,
  value?: ValidatedValue,
): ValidateResponse {
  const subject = resolveValidationSubject(schema, value);

  return schema.enum?.length
    ? validator(resolveRelaxedEnumValue(schema.enum, subject))
    : validator(subject);
}

/**
 * Checks if the configuration property value satisfies the given JSON schema.
 *
 * Values of `string` schemas are validated using their encoded representation. This allows
 * custom string formats, like `duration` or `data-size`, to be checked against their encoded
 * patterns. The same applies to the items of `array` schemas with `string` items, which are
 * validated using the encoded items. Values of all other schema types are validated using
 * their decoded representation.
 *
 * String values of `enum` schemas are matched using Spring Boot relaxed binding rules.
 *
 * @param schema the JSON schema of the configuration property
 * @param value the configuration property value to validate, can be `undefined`
 * @returns `true` when the value is valid, `false` otherwise
 */
export function isPropertyValueValid(schema: PropertyJsonSchema, value: ValidatedValue): boolean {
  const validator = createPropertyValidator(schema);
  return validatePropertyValue(validator, schema, value).valid;
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
    const response = validatePropertyValue(validator, schema, value);

    const nextResult: ValidationResult = response.valid
      ? buildValidationResult(true, [])
      : buildValidationResult(false, mapAjvErrors(response.err?.details ?? [], schema, intl));

    setResult(nextResult);
    return nextResult;
  }, [intl, validator]);

  return { validate, result };
}
