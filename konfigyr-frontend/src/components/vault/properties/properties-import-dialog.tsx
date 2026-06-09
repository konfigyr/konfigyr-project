import React, { useCallback, useState } from 'react';
import { z } from 'zod';
import { DatabaseBackup, FileCog, ImportIcon } from 'lucide-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
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
import { TabItem, Tabs } from '@konfigyr/components/ui/tab';
import { FetchConfigSchema } from '@konfigyr/hooks/vault/-handler';
import { ImportPropertiesLabel } from './messages';
import type { FetchConfigRequest } from '@konfigyr/hooks/vault/-handler';
import type { ConfigurationProperty, Profile } from '@konfigyr/hooks/types';

type ConfigurationImporterStatusProps = {
  isParsing: boolean;
  isError: boolean;
  error?: Error;
  hasProperties: boolean;
  amount: number;
};

type ImporterType = 'file' | 'api';

const DEFAULT_IMPORTER_TYPE: ImporterType = 'file';

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
              render={<field.Input type="text" autoComplete="username" />}
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
              render={<field.Input type="password" autoComplete="current-password" />}
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
              render={<field.Input type="text" placeholder="https://config.com/api/configs/app/profile/label" />}
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

export function PropertiesImportDialog ({ profile, onImport }: {
  onImport: (properties: Array<ConfigurationProperty<any>>) => void | Promise<unknown>
  profile: Profile
}) {
  const [open, setOpen] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importerType, setImporterType] = useState<ImporterType>(DEFAULT_IMPORTER_TYPE);
  const { properties, reset, error, parseFile, fetchConfig, isParsing, isError } = useConfigFileParser();

  const onOpenChange = useCallback((state: boolean) => {
    setOpen(state);
    setImporterType(DEFAULT_IMPORTER_TYPE);
    reset();
  }, [reset]);

  const handleImporterTypeChange = useCallback((type: ImporterType) => {
    setImporterType(type);
    reset();
  }, [reset]);

  const handleSubmit = useCallback(async () => {
    setIsImporting(true);
    await onImport(properties);
    setOpen(false);
    setIsImporting(false);
  }, [onImport, properties]);

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
          <SpringCloudConfigurationImporter onFetchConfig={fetchConfig} />
        )}

        <ConfigurationImporterStatus
          isParsing={isParsing}
          hasProperties={properties.length > 0}
          amount={properties.length}
          isError={isError}
          error={error}
        />

        <DialogFooter showCloseButton={true}>
          <Button disabled={isImportDisabled} onClick={handleSubmit}>
            <ImportIcon/>
            <ImportPropertiesLabel/>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ConfigurationImporterStatus ({ hasProperties, amount, isParsing, isError, error }: ConfigurationImporterStatusProps ) {
  const intl = useIntl();

  const noPropertiesTitle = intl.formatMessage({
    defaultMessage: 'No configuration properties for import',
    description: 'Empty state description shown when parsed configuration has no properties.',
  });

  return (
    <>
      {isParsing && (
        <ParseStatus
          title={
            intl.formatMessage({
              defaultMessage: 'Reading configuration',
              description: 'Status title shown while configuration file is being parsed.',
            })
          }
          description={
            intl.formatMessage({
              defaultMessage: 'Reading your configuration',
              description: 'Status description shown while configuration file is being parsed',
            })
          }
        />
      )}

      {isError && (
        <ParseStatus
          title={noPropertiesTitle}
          description={
            intl.formatMessage({
              defaultMessage: 'Could not read your configuration: {error}',
              description: 'Error message shown when configuration parsing fails',
            }, {
              error: error?.message,
            })
          }
        />
      )}

      {!hasProperties && !isError && (
        <ParseStatus
          title={noPropertiesTitle}
          description={
            intl.formatMessage({
              defaultMessage: 'Your configuration is empty',
              description: 'Empty state title shown when parsed configuration has no properties',
            })
          }
        />
      )}

      {hasProperties && !isError && (
        <ParseStatus
          title={
            intl.formatMessage({
              defaultMessage: 'Ready for import',
              description: 'State title shown when parsed configuration properties are ready',
            })
          }
          description={
            intl.formatMessage({
              defaultMessage: 'Your configuration was parsed. There are {amount} new configuration properties ready for import.',
              description: 'State description shown when parsed configuration properties are ready',
            }, {
              amount: amount,
            })
          }
        />
      )}
    </>
  );
}

function ParseStatus ({ title, description }: { title: string, description: string }) {
  return (
    <div className="mx-auto flex max-w-sm flex-col items-center gap-2 text-center">
      <div className="text-lg font-medium">{title}</div>
      <div className="text-sm/relaxed">{description}</div>
    </div>
  );
}
