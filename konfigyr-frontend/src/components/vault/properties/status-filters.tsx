'use client';

import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { labelForTransitionType } from '@konfigyr/components/vault/messages';
import { Button } from '@konfigyr/components/ui/button';
import { cn } from '@konfigyr/components/utils';

import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import type { ChangesetState } from '@konfigyr/hooks/types';

export const StatusFilter = {
  ALL: 'ALL',
  ...PropertyTransitionType,
};

export type StatusFilter = typeof StatusFilter[keyof typeof StatusFilter];

interface StatusFilterState {
  key: StatusFilter;
  count: number;
}

function useStatusLabel(label: StatusFilter): string | undefined {
  const intl = useIntl();

  switch (label) {
    case StatusFilter.ALL: return intl.formatMessage({
      defaultMessage: 'All',
      description: 'Default label for the changeset status filter used to show all properties.',
    });
    default:
      return labelForTransitionType(intl, label as PropertyTransitionType);
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
    key: StatusFilter.ALL,
    count: changeset.properties.length,
  }, {
    key: StatusFilter.UPDATED,
    count: changeset.modified,
  }, {
    key: StatusFilter.ADDED,
    count: changeset.added,
  }, {
    key: StatusFilter.REMOVED,
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
