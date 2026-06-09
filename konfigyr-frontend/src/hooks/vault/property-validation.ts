import { useCallback, useMemo, useState } from 'react';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import type { ErrorObject, ValidateFunction } from 'ajv';

import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { ValidationError, ValidationResult } from '@konfigyr/hooks/vault/types';

const ajv = new Ajv({ allErrors: true, coerceTypes: false });

addFormats(ajv);

ajv.addFormat('duration', /^[+-]?\d+(ns|us|ms|s|m|h|d)$/);
ajv.addFormat('data-size', /^[+-]?\d+(B|KB|MB|GB|TB)$/);

const mapAjvErrors = (errors: Array<ErrorObject>): Array<ValidationError> => {
  return errors.map(error => ({
    keyword: error.keyword,
    message: error.message ?? '',
    params: error.params,
  }));
};

const buildValidationResult = (isValid: boolean, errors: Array<ValidationError>) => ({ isValid, errors });

export function usePropertyValidation (schema: PropertyJsonSchema): {
  validate: (value: unknown) => ValidationResult;
  result: ValidationResult;
} {
  const validator = useMemo<ValidateFunction<unknown>>(() => ajv.compile(schema), [schema]);

  const [result, setResult] = useState<ValidationResult>(() => buildValidationResult(true, []));

  const validate = useCallback((value: unknown): ValidationResult => {
    const isValid = validator(value);

    const nextResult: ValidationResult = isValid
      ? buildValidationResult(true, [])
      : buildValidationResult(false, mapAjvErrors(validator.errors ?? []));

    setResult(nextResult);
    return nextResult;
  }, [validator]);

  return {
    validate,
    result,
  };
}
