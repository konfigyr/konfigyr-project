import { defineMessages } from 'react-intl';

export const VALIDATION_MESSAGES = defineMessages({
  type: {
    defaultMessage: 'Must be a valid {type}',
    description: 'Validation error shown when a configuration property value has the wrong type according to its JSON Schema.',
  },
  enum: {
    defaultMessage: 'Must be one of: {values}',
    description: 'Validation error shown when a configuration property value is not one of the allowed enum values defined in its JSON Schema.',
  },
  required: {
    defaultMessage: 'Missing required property: {property}',
    description: 'Validation error shown when a required configuration property is missing according to its JSON Schema.',
  },
  minimum: {
    defaultMessage: 'Must be greater than or equal to {limit}',
    description: 'Validation error shown when a numeric configuration property value is below the minimum defined in its JSON Schema.',
  },
  exclusiveMinimum: {
    defaultMessage: 'Must be greater than {limit}',
    description: 'Validation error shown when a numeric configuration property value does not exceed the exclusive minimum defined in its JSON Schema.',
  },
  maximum: {
    defaultMessage: 'Must be less than or equal to {limit}',
    description: 'Validation error shown when a numeric configuration property value exceeds the maximum defined in its JSON Schema.',
  },
  exclusiveMaximum: {
    defaultMessage: 'Must be less than {limit}',
    description: 'Validation error shown when a numeric configuration property value meets or exceeds the exclusive maximum defined in its JSON Schema.',
  },
  multipleOf: {
    defaultMessage: 'Must be a multiple of {value}',
    description: 'Validation error shown when a numeric configuration property value is not a multiple of the required divisor defined in its JSON Schema.',
  },
  minLength: {
    defaultMessage: 'Must be at least {limit} character(s)',
    description: 'Validation error shown when a string configuration property value is shorter than the minimum length defined in its JSON Schema.',
  },
  maxLength: {
    defaultMessage: 'Must be at most {limit} character(s)',
    description: 'Validation error shown when a string configuration property value exceeds the maximum length defined in its JSON Schema.',
  },
  pattern: {
    defaultMessage: 'Must match the required format',
    description: 'Validation error shown when a string configuration property value does not match the pattern regex defined in its JSON Schema. The raw regex is intentionally omitted from the message.',
  },
  format: {
    defaultMessage: 'Must be a valid {format}',
    description: 'Validation error shown when a configuration property value does not conform to the string format defined in its JSON Schema (e.g. date, time, duration, data-size).',
  },
  minItems: {
    defaultMessage: 'Must contain at least {limit} item(s)',
    description: 'Validation error shown when an array configuration property value contains fewer items than the minimum defined in its JSON Schema.',
  },
  maxItems: {
    defaultMessage: 'Must contain at most {limit} item(s)',
    description: 'Validation error shown when an array configuration property value contains more items than the maximum defined in its JSON Schema.',
  },
  uniqueItems: {
    defaultMessage: 'All items must be unique',
    description: 'Validation error shown when an array configuration property value contains duplicate items and the JSON Schema requires uniqueItems.',
  },
  invalid: {
    defaultMessage: 'Invalid value',
    description: 'Fallback validation error shown when Ajv reports a keyword without a dedicated localized message.',
  },
});
