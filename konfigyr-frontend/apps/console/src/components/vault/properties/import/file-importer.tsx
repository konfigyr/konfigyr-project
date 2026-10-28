import React from 'react';
import { FormattedMessage } from 'react-intl';
import { Field, FieldDescription, FieldLabel } from '@konfigyr/ui/components/field';
import { Input } from '@konfigyr/ui/components/input';
import { parseConfigFile } from '@konfigyr/hooks/vault/config-file-parser';
import type { PropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';

/**
 * Renders the file-based configuration importer.
 */
export function FileConfigurationImporter ({ importer }: {
  importer: PropertiesImporter,
}) {
  const inputId = React.useId();

  function handleChange (e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    // clear the input so that selecting the same file again triggers the change event
    e.target.value = '';
    if (file) {
      void importer(() => parseConfigFile(file));
    }
  }

  return (
    <Field>
      <FieldLabel htmlFor={inputId}>
        <FormattedMessage
          defaultMessage="Select existing configuration file"
          description="Label for the dialog that allows importing new configuration properties from a file"
        />
      </FieldLabel>
      <Input id={inputId} type="file" accept=".json,application/json,.properties,.yaml,.yml" onChange={handleChange}/>
      <FieldDescription>
        <FormattedMessage
          defaultMessage="Supported formats: <b>.json</b>, <b>.yaml</b>, <b>.yml</b> or <b>.properties</b>."
          description="Label describing the supported file formats for importing configuration properties."
          values={{
            b: (chunks) => <strong>{chunks}</strong>,
          }}
        />
      </FieldDescription>
    </Field>
  );
}
