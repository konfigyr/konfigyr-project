'use client';

import { useMemo, useState } from 'react';
import { SearchIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import {
  useAddProperty,
  useModifyProperty,
  useRemoveProperty,
  useRestoreProperty,
} from '@konfigyr/hooks';
import { Input } from '@konfigyr/components/ui/input';
import { ChangesetStatusBar } from '@konfigyr/components/vault/changeset/status-bar';
import { PropertyDialog } from '@konfigyr/components/vault/properties/property-dialog';
import { PropertyHistorySidebar } from '@konfigyr/components/vault/properties/history-sidebar';
import { PropertyStatusFilters } from '@konfigyr/components/vault/properties/status-filters';
import { PropertiesTable } from '@konfigyr/components/vault/properties/table';

import type { ChangesetState, ConfigurationProperty } from '@konfigyr/hooks/types';
import type { StatusFilter } from '@konfigyr/components/vault/properties/status-filters';

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
    if (statusFilter === 'all') {
      return changeset.properties;
    }
    return changeset.properties.filter(it => it.state === statusFilter);
  }, [changeset, statusFilter]);

  return useMemo(() => {
    const term = termFilter.toLowerCase().trim();

    if (term.length === 0) {
      return properties;
    }

    return properties.filter(({ name, description, value }) =>
      matches(term, name) || matches(term, description) || matches(term, value),
    );
  }, [properties, termFilter]);
};

function SearchFilter({ value, debounce = 200, onChange }: {
  value: string,
  debounce?: number,
  onChange: (value: string) => void,
}) {
  const onDebouncedChange = useDebouncedCallback(onChange, debounce);

  return (
    <div className="relative flex-1 max-w-sm">
      <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
      <Input
        placeholder="Filter properties..."
        defaultValue={value}
        onChange={event => onDebouncedChange(event.target.value)}
        className="pl-9 text-sm"
      />
    </div>
  );
}

export function ChangesetEditor({ changeset }: { changeset: ChangesetState }) {
  const [propertyStatusFilter, onPropertyStatusFilterChanged] = useState<StatusFilter>('all');
  const [propertyTermFilter, onPropertyTermFilterChanged] = useState<string>('');

  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyProperty, setHistoryProperty] = useState<ConfigurationProperty | null>(null);

  const { mutateAsync: onAddProperty } = useAddProperty(changeset);
  const { mutateAsync: onUpdateProperty } = useModifyProperty(changeset);
  const { mutateAsync: onDeleteProperty } = useRemoveProperty(changeset);
  const { mutateAsync: onRestoreProperty } = useRestoreProperty(changeset);

  const properties = useFilteredProperties(
    changeset,
    propertyTermFilter,
    propertyStatusFilter,
  );

  const onAdd = async (property: ConfigurationProperty) => {
    await onAddProperty({ property, value: property.value });
  };

  const onDelete = async (property: ConfigurationProperty) => {
    await onDeleteProperty({ property });
  };

  const onHistory = (property: ConfigurationProperty) => {
    setHistoryProperty(property);
    setHistoryOpen(true);
  };

  const onUpdate = async (property: ConfigurationProperty, value?: string) => {
    await onUpdateProperty({ property, value });
  };

  const onRestore = async (property: ConfigurationProperty) => {
    await onRestoreProperty({ property });
  };

  return (
    <div className="pb-14">
      <PropertyHistorySidebar
        open={historyOpen}
        onOpenChange={setHistoryOpen}
        property={historyProperty}
        profile={changeset.profile}
      />

      <div className="mb-6">
        <ChangesetStatusBar changeset={changeset} />
      </div>

      <div className="flex items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2 flex-1">
          <SearchFilter
            value={propertyTermFilter}
            onChange={onPropertyTermFilterChanged}
          />

          <PropertyDialog
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
