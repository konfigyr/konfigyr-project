'use client';

import { useMemo, useState } from 'react';
import {
  useAddProperty,
  useModifyProperty,
  useRemoveProperty,
  useRestoreProperty,
} from '@konfigyr/hooks';
import { ChangesetStatusBar } from '@konfigyr/components/vault/changeset/status-bar';
import { PropertyDialog } from '@konfigyr/components/vault/properties/property-dialog';
import { SearchInputGroup } from '@konfigyr/components/vault/properties/search-input-group';
import { PropertyHistorySidebar } from '@konfigyr/components/vault/properties/history-sidebar';
import { PropertyStatusFilters, StatusFilter } from '@konfigyr/components/vault/properties/status-filters';
import { PropertiesTable } from '@konfigyr/components/vault/properties/table';

import type {
  ChangesetState,
  ConfigurationProperty,
  ConfigurationPropertyValue,
  ServiceCatalog,
} from '@konfigyr/hooks/types';

const matches = (term: string, value?: string | undefined | null) => {
  if (value === undefined || value === null) {
    return false;
  }
  return value.toLowerCase().includes(term);
};

const useFilteredProperties = (
  changeset: ChangesetState,
  termFilter: string,
  statusFilter: StatusFilter,
) => {
  const properties = useMemo(() => {
    if (statusFilter === StatusFilter.ALL) {
      return changeset.properties;
    }
    return changeset.properties.filter(it => it.state === statusFilter);
  }, [changeset, statusFilter]);

  return useMemo(() => {
    const term = termFilter.toLowerCase().trim();

    if (term.length === 0) {
      return properties;
    }

    return properties.filter(({ name, description, value }) => (
      matches(term, name) || matches(term, description) || matches(term, value?.encoded)
    ));
  }, [properties, termFilter]);
};

export function ChangesetEditor({ catalog, changeset }: { catalog: ServiceCatalog, changeset: ChangesetState }) {
  const [propertyStatusFilter, onPropertyStatusFilterChanged] = useState<StatusFilter>(StatusFilter.ALL);
  const [propertyTermFilter, onPropertyTermFilterChanged] = useState<string>('');

  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyProperty, setHistoryProperty] = useState<ConfigurationProperty<any> | null>(null);

  const { mutateAsync: onAddProperty } = useAddProperty(changeset);
  const { mutateAsync: onUpdateProperty } = useModifyProperty(changeset);
  const { mutateAsync: onDeleteProperty } = useRemoveProperty(changeset);
  const { mutateAsync: onRestoreProperty } = useRestoreProperty(changeset);

  const properties = useFilteredProperties(
    changeset,
    propertyTermFilter,
    propertyStatusFilter,
  );

  const onAdd = async (property: ConfigurationProperty<any>) => {
    await onAddProperty({ property, value: property.value });
  };

  const onDelete = async (property: ConfigurationProperty<any>) => {
    await onDeleteProperty({ property });
  };

  const onHistory = (property: ConfigurationProperty<any>) => {
    setHistoryProperty(property);
    setHistoryOpen(true);
  };

  const onUpdate = async (property: ConfigurationProperty<any>, value?: ConfigurationPropertyValue<any>) => {
    await onUpdateProperty({ property, value });
  };

  const onRestore = async (property: ConfigurationProperty<any>) => {
    await onRestoreProperty({ property });
  };

  return (
    <div className="pb-14">
      <PropertyHistorySidebar
        open={historyOpen}
        onOpenChange={setHistoryOpen}
        property={historyProperty}
        namespace={changeset.namespace}
        service={changeset.service}
        profile={changeset.profile}
      />

      <div className="mb-6">
        <ChangesetStatusBar changeset={changeset} />
      </div>

      <div className="flex items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2 flex-1">
          <SearchInputGroup
            className="max-w-sm"
            value={propertyTermFilter}
            onChange={onPropertyTermFilterChanged}
          />

          <PropertyDialog
            catalog={catalog}
            changeset={changeset}
            onAdd={onAdd}
          />
        </div>

        <PropertyStatusFilters
          changeset={changeset}
          value={propertyStatusFilter}
          onChange={onPropertyStatusFilterChanged}
        />
      </div>

      <PropertiesTable
        properties={properties}
        onHistory={onHistory}
        onRestore={onRestore}
        onDelete={onDelete}
        onUpdate={onUpdate}
      />
    </div>
  );
}
