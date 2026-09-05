import type { ConfigurationProperty, ConfigurationPropertyState } from '@konfigyr/hooks/types';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';

/**
 * Builds a minimal configuration property fixture for tests.
 *
 * The `encoded` value is reused for `decoded` to keep the factory lightweight
 * when the test only cares about the property shape.
 *
 * @param name property name.
 * @param state property state used by the changeset.
 * @param schema JSON schema for the property value.
 * @param typeName fully qualified Java type name for the property.
 * @param encoded optional encoded value to attach to the property.
 */
export function buildConfigurationProperty(
  name: string,
  state: ConfigurationPropertyState,
  schema: PropertyJsonSchema,
  typeName?: string,
  encoded?: string,
): ConfigurationProperty<any> {
  return {
    name,
    typeName: typeName || 'java.lang.String',
    schema,
    state,
    value: encoded === undefined
      ? undefined
      : {
        encoded,
        decoded: encoded,
      },
  };
}
