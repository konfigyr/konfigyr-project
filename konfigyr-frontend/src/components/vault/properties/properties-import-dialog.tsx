import React, { useCallback, useState } from 'react';
import { CurlyBracesIcon, FileCog, ImportIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
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
import { Input } from '@konfigyr/components/ui/input';
import { useConfigFileParser } from '@konfigyr/hooks/vault/config-file-parser';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { ImportPropertiesLabel } from './messages';
import type { ConfigurationProperty } from '@konfigyr/hooks/types';

export function ConfigurationImporter ({ onChange }: {
  onChange: (file: File ) => void,
}) {
  const inputId = React.useId();

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
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

export function PropertiesImportDialog ({ onImport }: {
  onImport: (properties: Array<ConfigurationProperty<any>>) => void | Promise<unknown>
}) {
  const [open, setOpen] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [error, setError] = useState<string>();
  const { properties, parseFile, reset } = useConfigFileParser();

  const onOpenChange = useCallback((state: boolean) => {
    setOpen(state);
    setError(undefined);
    reset();
  }, [reset]);

  const handleFileChange = useCallback(async (file: File) => {
    setError(undefined);
    try {
      await parseFile(file);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Parse error');
    }
  }, [parseFile]);

  const handleSubmit = useCallback(async () => {
    setIsImporting(true);
    setError(undefined);
    try {
      await onImport(properties);
      setOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Import failed');
    } finally {
      setIsImporting(false);
    }
  }, [onImport, properties]);

  const isImportDisabled = properties.length === 0 || isImporting;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger
        render={
          <Button variant="outline">
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

        <ConfigurationImporter onChange={handleFileChange}/>
        {error && (
          <p className="text-sm text-destructive">
            {error}
          </p>
        )}

        {isImportDisabled
          ? <ConfigurationPropertiesEmpty />
          : <ConfigurationPropertiesReadyForImport amount={properties.length} />
        }

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

function ConfigurationPropertiesEmpty () {
  return (
    <EmptyState
      icon={
        <CurlyBracesIcon/>
      }
      title={
        <FormattedMessage
          defaultMessage="No configuration properties for import."
          description="Empty state title used when no configuration propertis are found."
        />
      }
      description={
        <FormattedMessage
          defaultMessage="Import new properties from existing configuration file. It will create new configuration properties or overwrite existing values. Files in unsupported formats will be rejected."
          values={{
            b: (chunks) => <strong> {chunks}</strong>,
          }}
          description="Label prompting an user to import configuration properties from a file."
        />
      }
    />
  );
}

function ConfigurationPropertiesReadyForImport ({ amount }: { amount: number }) {
  return (
    <EmptyState
      icon={
        <CurlyBracesIcon/>
      }
      title={
        <FormattedMessage
          defaultMessage="Ready for import"
          description="Empty state title used when no configuration propertis are found."
        />
      }
      description={
        <FormattedMessage
          defaultMessage="There are {amount} new configuration properties ready for import. The import will create new properties or overwrite existing values."
          values={{ amount }}
          description="Label prompting an user to import configuration properties from a file."
        />
      }
    />
  );
}
