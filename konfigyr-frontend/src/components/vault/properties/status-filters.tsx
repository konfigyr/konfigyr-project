'use client';

import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import { cn } from '@konfigyr/components/utils';

import type { ChangesetState } from '@konfigyr/hooks/types';

export type StatusFilter = 'all' | 'modified' | 'added' | 'deleted';

interface StatusFilterState {
  key: StatusFilter;
  count: number;
}

function useStatusLabel(label: StatusFilter): string | undefined {
  const intl = useIntl();

  switch (label) {
    case 'all': return intl.formatMessage({
      defaultMessage: 'All',
      description: 'Default label for the changeset status filter used to show all properties.',
    });
    case 'modified': return intl.formatMessage({
      defaultMessage: 'Modified',
      description: 'Default label for the changeset status filter used to show only modified properties.',
    });
    case 'added': return intl.formatMessage({
      defaultMessage: 'Added',
      description: 'Default label for the changeset status filter used to show only added properties.',
    });
    case 'deleted': return intl.formatMessage({
      defaultMessage: 'Deleted',
      description: 'Default label for the changeset status filter used to show only deleted properties.',
    });
  }
}

function PropertyStatusFilter({ value, count, active, onChange }: {
  value: StatusFilter,
  count: number,
  active: boolean,
  onChange: (value: StatusFilter) => void
}) {
  const label = useStatusLabel(value);

  return (
    <Button
      role="radio"
      aria-label={label}
      aria-checked={active}
      size="sm"
      variant="outline"
      onClick={() => onChange(value)}
      className={cn(active && 'bg-foreground! text-background!')}
    >
      {label}
      <span className={cn('tabular-nums', active ? 'text-background/80' : 'text-muted-foreground/70')}>
        {count}
      </span>
    </Button>
  );
}

export function PropertyStatusFilters({ changeset, value = 'all', onChange }: {
  changeset: ChangesetState,
  value?: StatusFilter,
  onChange: (value: StatusFilter) => void,
}) {
  const states: Array<StatusFilterState> = useMemo(() => [{
    key: 'all',
    count: changeset.properties.length,
  }, {
    key: 'modified',
    count: changeset.modified,
  }, {
    key: 'added',
    count: changeset.added,
  }, {
    key: 'deleted',
    count: changeset.deleted,
  }], [changeset]);

  return (
    <div
      role="radiogroup"
      aria-label="Property status filters"
      className="flex items-center gap-1"
    >
      {states.map((state) => (
        <PropertyStatusFilter
          key={state.key}
          value={state.key}
          count={state.count}
          active={value === state.key}
          onChange={onChange}
        />
      ))}
    </div>
  );
}
