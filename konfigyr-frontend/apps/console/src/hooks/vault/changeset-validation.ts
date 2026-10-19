'use client';

import { useMemo } from 'react';
import { isPropertyValueValid } from '@konfigyr/hooks/vault/property-validation';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import type { ConfigurationProperty } from '@konfigyr/hooks/types';

/**
 * Hook that validates the values of all changed configuration properties in a changeset.
 *
 * Properties in the `REMOVED` or `UNCHANGED` state are skipped. All other properties are
 * validated against their JSON schema using {@link isPropertyValueValid}.
 *
 * The result is memoized and recalculated only when the properties change.
 *
 * @param properties the configuration properties of the changeset
 * @returns the names of properties with invalid values and if the changeset is valid
 */
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

      if (!isPropertyValueValid(property.schema, property.value)) {
        invalidPropertyNames.add(property.name);
      }
    });

    return {
      invalidPropertyNames,
      isChangesetValid: invalidPropertyNames.size === 0,
    };
  }, [properties]);
}
