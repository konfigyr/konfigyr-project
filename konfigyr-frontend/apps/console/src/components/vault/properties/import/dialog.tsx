import { useCallback, useMemo, useState } from 'react';
import { FileCog, ImportIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { CloseLabel } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/ui/components/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/ui/components/dialog';
import { usePropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';
import { TabItem, Tabs } from '@konfigyr/ui/components/tab';
import { ImportPropertiesLabel } from '../messages';
import { FileConfigurationImporter } from './file-importer';
import { ProfileConfigurationImporter } from './profile-importer';
import { SpringCloudConfigurationImporter } from './spring-cloud-importer';
import { ConfigurationImporterStatus } from './status';
import { categorizePropertiesByValidation } from './validation';
import type { ConfigurationProperty, Profile, ServiceCatalog } from '@konfigyr/hooks/types';

type ImporterType = 'file' | 'api' | 'profile';

const DEFAULT_IMPORTER_TYPE: ImporterType = 'file';

/**
 * Opens the import dialog and coordinates parsing, validation, and submission.
 */
export function PropertiesImportDialog ({ catalog, profile, isPending = false, onImport }: {
  onImport: (properties: Array<ConfigurationProperty<any>>) => void | Promise<unknown>
  catalog: ServiceCatalog
  profile: Profile
  isPending?: boolean
}) {
  const [open, setOpen] = useState(false);
  const [importerType, setImporterType] = useState<ImporterType>(DEFAULT_IMPORTER_TYPE);
  const { properties, reset, error, importer, isImporting, isError } = usePropertiesImporter();

  const categorizedProperties = useMemo(() => categorizePropertiesByValidation(properties, catalog), [properties, catalog]);

  const hasValidationErrors = categorizedProperties.invalid.length > 0;
  const needsImportModeSelection = properties.length > 0 && !isImporting && !isError && hasValidationErrors;

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
    await onImport(importedProperties);
    onOpenChange(false);
  }, [onImport, onOpenChange]);

  const isImportDisabled = properties.length === 0 || isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger
        render={
          <Button disabled={profile.policy === 'IMMUTABLE'}>
            <ImportIcon data-icon="icon-start"/>
            <ImportPropertiesLabel/>
          </Button>
        }
      />
      <DialogContent className="sm:max-w-140" closeLabel={<CloseLabel />}>
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
          <TabItem
            render={
              <button
                type="button"
                data-state={importerType === 'profile' ? 'active' : undefined}
                onClick={() => handleImporterTypeChange('profile')}
              >
                <FormattedMessage
                  defaultMessage="From other profile"
                  description="Tab label for importing configuration properties from an other Application profile."
                />
              </button>
            }
          />
        </Tabs>

        {importerType === 'file' && (
          <FileConfigurationImporter importer={importer}/>
        )}

        {importerType === 'api' && (
          <SpringCloudConfigurationImporter importer={importer}/>
        )}

        {importerType === 'profile' && (
          <ProfileConfigurationImporter importer={importer} currentService={catalog.service} currentProfile={profile}/>
        )}

        <ConfigurationImporterStatus
          isParsing={isImporting}
          valid={categorizedProperties.valid.length}
          invalid={categorizedProperties.invalid.length}
          isError={isError}
          error={error}
        />

        <DialogFooter showCloseButton={true} closeLabel={<CloseLabel />}>
          {needsImportModeSelection ? (
            <>
              <Button
                disabled={isPending || categorizedProperties.valid.length === 0}
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
                disabled={isPending}
                onClick={() => handleImport([...categorizedProperties.valid, ...categorizedProperties.invalid])}
              >
                <ImportIcon/>
                <FormattedMessage
                  defaultMessage="Import all anyway"
                  description="Button label that imports all properties during the import review step, including properties that failed validation."
                />
              </Button>
            </>
          ) : (
            <Button disabled={isImportDisabled} onClick={() => handleImport(categorizedProperties.valid)}>
              <ImportIcon/>
              <ImportPropertiesLabel/>
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
