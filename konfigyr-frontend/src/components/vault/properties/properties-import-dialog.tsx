import React, { useCallback, useMemo, useState } from 'react';
import { DatabaseBackup, FileCog, ImportIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import { SimpleAlert } from '@konfigyr/components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import { Field, FieldDescription, FieldLabel } from '@konfigyr/components/ui/field';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { Input } from '@konfigyr/components/ui/input';
import { useConfigFileParser } from '@konfigyr/hooks/vault/config-file-parser';
import { isPropertyValueValid } from '@konfigyr/hooks/vault/property-validation';
import { TabItem, Tabs } from '@konfigyr/components/ui/tab';
import { FetchConfigSchema } from '@konfigyr/hooks/vault/-handler';
import { ImportPropertiesLabel } from './messages';
import type { FetchConfigRequest } from '@konfigyr/hooks/vault/-handler';
import type { ConfigurationProperty, Profile, ServiceCatalog, ValidationResult } from '@konfigyr/hooks/types';

type ImporterType = 'file' | 'api';

const DEFAULT_IMPORTER_TYPE: ImporterType = 'file';

type ConfigurationImporterStatusProps = {
  isParsing: boolean;
  isError: boolean;
  error?: Error;
  valid: number;
  invalid: number;
};

export interface PropertyValidationSummary {
  valid: Array<ConfigurationProperty<any>>;
  invalid: Array<ConfigurationProperty<any>>;
}

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

    const isValid = isPropertyValueValid(descriptor.schema, property.value?.encoded);

    property.description = descriptor.description;
    property.schema = descriptor.schema;

    if (isValid) {
      valid.push(property);
      continue;
    }

    invalid.push(property);
  }

  return { valid, invalid };
}

export function FileConfigurationImporter ({ onChange }: {
  onChange: (file: File) => void,
}) {
  const inputId = React.useId();

  function handleChange (e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      onChange(file);
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
            b: (chunks) => <strong> {chunks}</strong>,
          }}
        />
      </FieldDescription>
    </Field>
  );
}

export function SpringCloudConfigurationImporter ({ onFetchConfig }: {
  onFetchConfig: (payload: FetchConfigRequest) => void | Promise<unknown>
}) {
  const form = useForm({
    defaultValues: {
      username: '',
      password: '',
      configServerUrl: '',
    },
    validators: {
      onSubmit: FetchConfigSchema,
    },
    onSubmit: async ({ value }) => {
      await onFetchConfig({
        username: value.username.trim(),
        password: value.password.trim(),
        configServerUrl: value.configServerUrl.trim(),
      });
    },
  });

  const onSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form onSubmit={onSubmit} className="space-y-4">
        <form.AppField
          name="username"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="User name"
                  description="Label for username input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="text" autoComplete="username"/>}
            />
          )}
        />

        <form.AppField
          name="password"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="Password"
                  description="Label for password input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="password" autoComplete="current-password"/>}
            />
          )}
        />

        <form.AppField
          name="configServerUrl"
          children={(field) => (
            <field.Control
              label={(
                <FormattedMessage
                  defaultMessage="Config server URL"
                  description="Label for config server URL input in Spring Cloud configuration importer."
                />
              )}
              render={<field.Input type="text" placeholder="https://config.com/api/configs/app/profile/label"/>}
            />
          )}
        />

        <form.Submit variant="default">
          <DatabaseBackup/>
          <FormattedMessage
            defaultMessage="Fetch config"
            description="Button label for fetching configuration from Spring Cloud Config server."
          />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}

export function PropertiesImportDialog ({ catalog, profile, onImport }: {
  onImport: (properties: Array<ConfigurationProperty<any>>) => void | Promise<unknown>
  catalog: ServiceCatalog
  profile: Profile
}) {
  const [open, setOpen] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importerType, setImporterType] = useState<ImporterType>(DEFAULT_IMPORTER_TYPE);
  const { properties, reset, error, parseFile, fetchConfig, isParsing, isError } = useConfigFileParser();

  const categorizedProperties = useMemo(() => categorizePropertiesByValidation(properties, catalog), [properties, catalog]);

  const hasValidationErrors = categorizedProperties.invalid.length > 0;
  const needsImportModeSelection = properties.length > 0 && !isParsing && !isError && hasValidationErrors;

  const onOpenChange = useCallback((state: boolean) => {
    setOpen(state);
    setImporterType(DEFAULT_IMPORTER_TYPE);
    reset();
  }, [reset]);

  const handleImporterTypeChange = useCallback((type: ImporterType) => {
    setImporterType(type);
    reset();
  }, [reset]);

  const handleImport = useCallback(async (importedProperties: Array<ConfigurationProperty<any>>) => {
    setIsImporting(true);
    try {
      await onImport(importedProperties);
      setOpen(false);
    } finally {
      setIsImporting(false);
    }
  }, [onImport]);

  const isImportDisabled = properties.length === 0 || isImporting;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger
        render={
          <Button variant="outline" disabled={profile.policy === 'IMMUTABLE'}>
            <ImportIcon/>
            <ImportPropertiesLabel/>
          </Button>
        }
      />
      <DialogContent className="sm:max-w-140">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-lg">
            <FileCog size="1rem"/>
            <FormattedMessage
              defaultMessage="Import configuration properties"
              description="Label for the dialog that allows importing new configuration properties from a file (e.g., JSON, YAML)"
            />
          </DialogTitle>
        </DialogHeader>

        <Tabs>
          <TabItem
            render={
              <button
                type="button"
                data-state={importerType === 'file' ? 'active' : undefined}
                onClick={() => handleImporterTypeChange('file')}
              >
                <FormattedMessage
                  defaultMessage="From file"
                  description="Tab label for importing configuration properties from a file."
                />
              </button>
            }
          />
          <TabItem
            render={
              <button
                type="button"
                data-state={importerType === 'api' ? 'active' : undefined}
                onClick={() => handleImporterTypeChange('api')}
              >
                <FormattedMessage
                  defaultMessage="From config server"
                  description="Tab label for importing configuration properties from an Config Server."
                />
              </button>
            }
          />
        </Tabs>

        {importerType === 'file' && (
          <FileConfigurationImporter onChange={parseFile}/>
        )}

        {importerType === 'api' && (
          <SpringCloudConfigurationImporter onFetchConfig={fetchConfig}/>
        )}

        <ConfigurationImporterStatus
          isParsing={isParsing}
          valid={categorizedProperties.valid.length}
          invalid={categorizedProperties.invalid.length}
          isError={isError}
          error={error}
        />

        <DialogFooter showCloseButton={true}>
          {needsImportModeSelection ? (
            <>
              <Button
                disabled={isImporting || categorizedProperties.valid.length === 0}
                onClick={() => handleImport(categorizedProperties.valid)}
              >
                <ImportIcon/>
                <FormattedMessage
                  defaultMessage="Import valid only"
                  description="Button label that imports only the properties that passed validation during the import review step."
                />
              </Button>

              <Button
                variant="destructive"
                disabled={isImporting}
                onClick={() => handleImport(properties)}
              >
                <ImportIcon/>
                <FormattedMessage
                  defaultMessage="Import all anyway"
                  description="Button label that imports all properties during the import review step, including properties that failed validation."
                />
              </Button>
            </>
          ) : (
            <Button disabled={isImportDisabled} onClick={() => handleImport(properties)}>
              <ImportIcon/>
              <ImportPropertiesLabel/>
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ConfigurationImporterStatus ({ valid, invalid, isParsing, isError, error }: ConfigurationImporterStatusProps) {
  const hasProperties = !!valid || !!invalid;
  return (
    <>
      {isParsing && (
        <ReadingConfigurationAlert />
      )}

      {isError && (
        <ParsingErrorAlert error={error?.message}/>
      )}

      {!hasProperties && !isError && (
        <EmptyAlert />
      )}

      {hasProperties && !isError && (
        <>
          <InvalidPropertiesAlert count={invalid}/>
          <ValidPropertiesAlert count={valid}/>
        </>
      )}
    </>
  );
}

function InvalidPropertiesAlert ({ count }: { count: number }) {
  if (!count) {
    return;
  }
  return (
    <SimpleAlert
      variant="warning"
      title={(
        <FormattedMessage
          defaultMessage="Review imported properties"
          description="Title shown when imported configuration contains validation errors and requires user review before import."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Found {count} invalid properties. Continue with valid properties only, or import all and resolve issues afterward."
          description="Description shown in the import review warning when imported configuration contains invalid values."
          values={{ count }}
        />
      )}
    />
  );
}

function ValidPropertiesAlert ({ count }: { count: number }) {
  if (!count) {
    return;
  }
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="Ready for import"
          description="Title shown when valiad and ready for import properties"
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="The configuration was parsed, and {count} new configuration properties are available for import."
          description="Success message shown when the configuration is parsed and new properties are available for import."
          values={{ count }}
        />
      )}
    />
  );
}

function EmptyAlert () {
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="No configuration properties for import"
          description="Empty state description shown when parsed configuration has no properties."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Your configuration is empty"
          description="Empty state title shown when parsed configuration has no properties."
        />
      )}
    />
  );
}

function ReadingConfigurationAlert () {
  return (
    <SimpleAlert
      variant="default"
      title={(
        <FormattedMessage
          defaultMessage="Reading configuration"
          description="Status title shown while configuration file is being parsed."
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Reading your configuration"
          description="Status description shown while configuration file is being parsed"
        />
      )}
    />
  );
}

function ParsingErrorAlert ({ error }: { error?: string }) {
  return (
    <SimpleAlert
      variant="destructive"
      title={(
        <FormattedMessage
          defaultMessage="Could not read your configuration"
          description="Error message shown when configuration parsing fails"
        />
      )}
      description={(
        <FormattedMessage
          defaultMessage="Error: {error}"
          description="Error message shown when configuration parsing fails'"
          values={{ error }}
        />
      )}
    />
  );
}

