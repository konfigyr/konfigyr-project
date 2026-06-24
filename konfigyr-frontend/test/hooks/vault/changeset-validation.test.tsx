import { renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { useChangesetValidation } from '@konfigyr/hooks/vault/changeset-validation';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import { ConfigurationProperty, PropertyJsonSchema } from '@konfigyr/hooks/types';

const buildProperty = (
  name: string,
  state: ConfigurationPropertyState,
  schema: PropertyJsonSchema,
  typeName: string,
  encoded?: string,
): ConfigurationProperty<any> => ({
  name,
  typeName,
  schema,
  state,
  value: encoded === undefined
    ? undefined
    : {
        encoded,
        decoded: encoded,
      },
});

describe('hooks | vault | useChangesetValidation', () => {

  test('should ignore unchanged and removed properties', () => {
    const properties: Array<ConfigurationProperty<any>> = [
      buildProperty('spring.application.name', ConfigurationPropertyState.ADDED, { type: 'string' }, 'java.lang.String','demo'),
      buildProperty('spring.aop.auto', ConfigurationPropertyState.UNCHANGED, { type: 'boolean' }, 'java.lang.Boolean', 'truee'),
      buildProperty('spring.profiles.validate', ConfigurationPropertyState.REMOVED, { type: 'boolean' }, 'java.lang.Boolean','truee'),
    ];

    const { result } = renderHook(() => useChangesetValidation(properties));

    expect(result.current.isChangesetValid).toBe(true);
    expect(result.current.invalidPropertyNames).toStrictEqual(new Set());
  });

  test('should flag invalid added and updated properties', () => {
    const properties: Array<ConfigurationProperty<any>> = [
      buildProperty('spring.application.name', ConfigurationPropertyState.ADDED, { type: 'string', minLength: 3 }, 'java.lang.String','ok'),
      buildProperty('spring.aop.auto', ConfigurationPropertyState.UPDATED, { type: 'boolean' }, 'java.lang.Boolean','truee'),
    ];

    const { result } = renderHook(() => useChangesetValidation(properties));

    expect(result.current.isChangesetValid).toBe(false);
    expect(result.current.invalidPropertyNames).toStrictEqual(new Set(['spring.aop.auto', 'spring.application.name']));
  });

});
