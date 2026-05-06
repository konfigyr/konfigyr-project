import React, { useCallback, useMemo, useState } from 'react';
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

export function ConfigurationImporter ({ error, onChange }: {
  error?: string
  onChange: (file: File ) => void,
}) {

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      onChange(file);
    }
  }

  return (
    <Field>
      <FieldLabel htmlFor="json">
        <FormattedMessage
          defaultMessage="Select existing configuration file"
          description="Label for the dialog that allows importing new configuration properties from a file"
        />
      </FieldLabel>
      <Input id="json" type="file" accept=".json,application/json,.properties,.yaml,.yml" onChange={handleChange}/>
      <FieldDescription>
        {error
          ?
          <span className="text-destructive">{error}</span>
          :
          <FormattedMessage
            defaultMessage="Supported formats: <b>.json</b>, <b>.yaml</b>, <b>.yml</b> or <b>.properties</b>."
            description="Label describing the supported file formats for importing configuration properties."
            values={{
              b: (chunks) => <strong> {chunks}</strong>,
            }}
          />
        }
      </FieldDescription>
    </Field>
  );
}

export function PropertiesImportDialog ({ onImport }: {
  onImport: (properties: Array<ConfigurationProperty<any>>) => void | Promise<unknown>
}) {
  const [open, setOpen] = useState(false);
  const { properties, error, parseFile, reset } = useConfigFileParser();

  const onOpenChange = useCallback((state: boolean) => {
    setOpen(state);
    reset();
  }, [properties]);

  const handleSubmit = useCallback(() => {
    onImport(properties);
    setOpen(false);
  }, [properties]);

  const amount = useMemo(() => properties.length, [properties]);

  const isImportDisabled = useMemo(() => properties.length === 0, [properties]);

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

        <ConfigurationImporter error={error} onChange={parseFile}/>

        {isImportDisabled
          ? <ConfigurationPropertiesEmpty />
          : <ConfigurationPropertiesReadyForImport amount={amount} />
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
          values={{ amount: <strong>{amount}</strong> }}
          description="Label prompting an user to import configuration properties from a file."
        />
      }
    />
  );
}