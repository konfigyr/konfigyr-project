import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  DatePicker,
  DatePickerCalendar,
  DatePickerTrigger,
} from '@konfigyr/components/ui/calendar';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';

import { useAuditEntityTypeLabel } from './audit-entity-type';

import type { ReactNode } from 'react';
import type { AuditRecordQuery } from '@konfigyr/hooks/types';
import type { DateRange } from '@konfigyr/components/ui/calendar';

const ENTITY_TYPES = [
  'namespace',
  'namespace-application',
  'keyset',
  'service',
  'profile',
];

const MAX_DATE = new Date();

const DATE_FORMAT_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
};

interface FormFields {
  entityType?: string;
  range?: DateRange;
}

interface AuditRecordFiltersProps {
  query: AuditRecordQuery;
  onQueryChange: (query: AuditRecordQuery) => void;
  debounceMs?: number;
}

function useFormFields(query: AuditRecordQuery): FormFields {
  return useMemo(() => ({
    entityType: query.entityType || '',
    range: query.from ? {
      from: new Date(query.from),
      to: query.to ? new Date(query.to) : undefined,
    } : undefined,
  }), [query.entityType, query.from, query.to]);
}

export function AuditRecordFilters({ query, onQueryChange, debounceMs = 200 }: AuditRecordFiltersProps) {
  const intl = useIntl();
  const form = useForm({
    defaultValues: useFormFields(query),
    listeners: {
      onChangeDebounceMs: debounceMs,
      onChange: ({ formApi }) => formApi.handleSubmit(),
    },
    onSubmit: ({ value }) => {
      const from = value.range?.from;
      const to = value.range?.to;

      // react-day-picker sets from === to after the first click in range mode;
      // this is an intermediate state while the user is selecting the end date.
      if (from && to && from.getTime() === to.getTime()) {
        return;
      }

      onQueryChange({
        ...query,
        entityType: value.entityType ? value.entityType : undefined,
        from: from ? from.toISOString() : undefined,
        to: to ? to.toISOString() : undefined,
      });
    },
  });

  const onSubmit = useFormSubmit(form);

  const formatDateRange = (from?: Date, to?: Date): ReactNode => {
    const messages: Array<ReactNode> = [];

    if (from) {
      messages.push(intl.formatDate(from, DATE_FORMAT_OPTIONS));
    }

    if (to) {
      messages.push(intl.formatDate(to, DATE_FORMAT_OPTIONS));
    }

    if (messages.length === 0) {
      return (
        <span className="text-muted-foreground">
          {intl.formatMessage({
            defaultMessage: 'Select date range',
            description: 'Label for the audit record filter by date range dropdown.',
          })}
        </span>
      );
    }

    return messages.join(' - ');
  };

  const entityTypeLabelFor = useAuditEntityTypeLabel();
  const filterByEntityTypeLabel = intl.formatMessage({
    defaultMessage: 'Filter by entity type',
    description: 'Label for the audit record filter by entity tyoe dropdown.',
  });

  return (
    <form.AppForm>
      <form name="audit-record-filters" className="flex justify-end gap-4 grow" onSubmit={onSubmit}>
        <form.AppField
          name="entityType"
          children={(field) => (
            <Select
              value={field.state.value}
              onValueChange={it => field.handleChange(it ?? '')}
            >
              <SelectTrigger
                className="w-52"
                aria-label={filterByEntityTypeLabel}
              >
                <SelectValue>
                  {field.state.value ? entityTypeLabelFor(field.state.value) : filterByEntityTypeLabel}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel reset={true} onReset={() => field.handleChange('')}>
                    {filterByEntityTypeLabel}
                  </SelectLabel>

                  {Object.values(ENTITY_TYPES).map(option => (
                    <SelectItem key={option} value={option}>
                      {entityTypeLabelFor(option)}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          )}
        />

        <form.AppField
          name="range"
          children={(field) => (
            <DatePicker>
              <DatePickerTrigger
                render={
                  <Button variant="outline">
                    {formatDateRange(field.state.value?.from, field.state.value?.to)}
                  </Button>
                }
              />
              <DatePickerCalendar
                mode="range"
                selected={field.state.value}
                numberOfMonths={2}
                endMonth={MAX_DATE}
                disabled={{ after: MAX_DATE }}
                onSelect={field.handleChange}
              />
            </DatePicker>
          )}
        />
      </form>
    </form.AppForm>
  );
}
