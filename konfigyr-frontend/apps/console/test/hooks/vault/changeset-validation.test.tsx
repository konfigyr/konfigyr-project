import { renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { useChangesetValidation } from '@konfigyr/hooks/vault/changeset-validation';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import { buildConfigurationProperty } from '@konfigyr/test/helpers/factories/property';
import type { ConfigurationProperty } from '@konfigyr/hooks/types';


describe('hooks | vault | useChangesetValidation', () => {

  test('should ignore unchanged and removed properties', () => {
    const properties: Array<ConfigurationProperty<any>> = [
      buildConfigurationProperty('spring.application.name', ConfigurationPropertyState.ADDED, { type: 'string' }, 'java.lang.String','demo'),
      buildConfigurationProperty('spring.aop.auto', ConfigurationPropertyState.UNCHANGED, { type: 'boolean' }, 'java.lang.Boolean', 'truee'),
      buildConfigurationProperty('spring.profiles.validate', ConfigurationPropertyState.REMOVED, { type: 'boolean' }, 'java.lang.Boolean','truee'),
    ];

    const { result } = renderHook(() => useChangesetValidation(properties));

    expect(result.current.isChangesetValid).toBe(true);
    expect(result.current.invalidPropertyNames).toStrictEqual(new Set());
  });

  test('should flag invalid added and updated properties', () => {
    const properties: Array<ConfigurationProperty<any>> = [
      buildConfigurationProperty('spring.application.name', ConfigurationPropertyState.ADDED, { type: 'string', minLength: 3 }, 'java.lang.String','ok'),
      buildConfigurationProperty('spring.aop.auto', ConfigurationPropertyState.UPDATED, { type: 'boolean' }, 'java.lang.Boolean','truee'),
    ];

    const { result } = renderHook(() => useChangesetValidation(properties));

    expect(result.current.isChangesetValid).toBe(false);
    expect(result.current.invalidPropertyNames).toStrictEqual(new Set(['spring.aop.auto', 'spring.application.name']));
  });

});
