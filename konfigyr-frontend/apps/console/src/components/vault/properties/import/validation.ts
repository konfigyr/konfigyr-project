import { resolveJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import { isPropertyValueValid } from '@konfigyr/hooks/vault/property-validation';
import type { ConfigurationProperty, ServiceCatalog } from '@konfigyr/hooks/types';

export interface PropertyValidationSummary {
  valid: Array<ConfigurationProperty<any>>;
  invalid: Array<ConfigurationProperty<any>>;
}

/**
 * Splits imported properties into valid and invalid groups using the service catalog.
 *
 * Properties without a matching catalog descriptor are treated as valid so they can
 * still be imported.
 */
export function categorizePropertiesByValidation (
  properties: Array<ConfigurationProperty<any>>,
  catalog: ServiceCatalog,
): PropertyValidationSummary {
  const valid: Array<ConfigurationProperty<any>> = [];
  const invalid: Array<ConfigurationProperty<any>> = [];

  const descriptors = new Map(catalog.properties.map(property => [property.name, property]));

  for (const property of properties) {
    const descriptor = descriptors.get(property.name);

    if (!descriptor) {
      valid.push(property);
      continue;
    }

    const transform = resolveJsonSchemeTransform(descriptor.schema);
    const encoded = property.value?.encoded ?? '';
    const decoded = transform.decode(encoded);

    const { artifact: ignore, ...propertyDescriptor } = descriptor;
    const importedProperty: ConfigurationProperty<any> = {
      ...propertyDescriptor,
      state: property.state,
      value: { decoded, encoded },
    };

    if (isPropertyValueValid(descriptor.schema, importedProperty.value)) {
      valid.push(importedProperty);
    } else {
      invalid.push(importedProperty);
    }
  }

  return { valid, invalid };
}
