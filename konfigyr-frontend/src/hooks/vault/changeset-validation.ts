'use client';

import { useMemo } from 'react';
import { isPropertyValueValid } from '@konfigyr/hooks/vault/property-validation';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import type { ConfigurationProperty } from '@konfigyr/hooks/types';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

export function validatePropertyValue(schema: PropertyJsonSchema, encoded: unknown): boolean {
  return isPropertyValueValid(schema, encoded);
}

export function useChangesetValidation(properties: Array<ConfigurationProperty<any>>): {
  invalidPropertyNames: Set<string>;
  isChangesetValid: boolean;
} {
  return useMemo(() => {
    const invalidPropertyNames = new Set<string>();

    properties.forEach((property) => {
      if (property.state === ConfigurationPropertyState.REMOVED || property.state === ConfigurationPropertyState.UNCHANGED) {
        return;
      }

      if (!validatePropertyValue(property.schema, property.value?.decoded)) {
        invalidPropertyNames.add(property.name);
      }
    });

    return {
      invalidPropertyNames,
      isChangesetValid: invalidPropertyNames.size === 0,
    };
  }, [properties]);
}
