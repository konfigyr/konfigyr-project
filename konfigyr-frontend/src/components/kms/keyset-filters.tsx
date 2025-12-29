import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';
import { useForm } from '@konfigyr/components/ui/form';
import { KeysetAlgorithmSelect } from './keyset-algorithm';
import { KeysetStateSelect } from './keyset-state';

import type { FormEvent } from 'react';
import type { KeysetSearchQuery, KeysetState } from '@konfigyr/hooks/types';

export function KeysetFilters({ query, onQueryChange }: { query: KeysetSearchQuery, onQueryChange: (query: KeysetSearchQuery) => void }) {
  const form = useForm({
    defaultValues: {
      term: query.term || '',
      state: query.state || '',
      algorithm: query.algorithm || '',
      sort: query.sort || 'date',
    },
    listeners: {
      onChangeDebounceMs: 200,
      onChange: ({ formApi }) => formApi.handleSubmit(),
    },
    onSubmit: ({ value }) => onQueryChange({
      term: value.term ? value.term : undefined,
      sort: value.sort ? value.sort : undefined,
      state: value.state ? value.state as KeysetState: undefined,
      algorithm: value.algorithm ? value.algorithm : undefined,
    }),
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit();
  }, [form.handleSubmit]);

  return (
    <form.AppForm>
      <form name="keyset-filters" className="flex gap-4 flex-grow-1" onSubmit={onSubmit}>
        <form.AppField
          name="term"
          listeners={{
            onBlurDebounceMs: 200,
            onChangeDebounceMs: 200,
          }}
          children={(field) => (
            <field.Control className="flex-grow-1">
              <field.Input
                type="search"
                placeholder="Search keysets..."
                aria-label="Search keysets"
              />
            </field.Control>
          )}
        />

        <form.AppField
          name="state"
          children={(field) => (
            <KeysetStateSelect
              value={field.state.value}
              reset={!!field.state.value}
              placeholder={<FormattedMessage
                defaultMessage="Filter by state"
                description="Placeholder for the keyset state sort field"
              />}
              onChange={field.handleChange}
              onReset={() => field.handleChange('')}
            />
          )}
        />

        <form.AppField
          name="algorithm"
          children={(field) => (
            <KeysetAlgorithmSelect
              value={field.state.value}
              reset={!!field.state.value}
              placeholder={<FormattedMessage
                defaultMessage="Filter by algorithm"
                description="Placeholder for the keyset algorithm sort field"
              />}
              onChange={field.handleChange}
              onReset={() => field.handleChange('')}
            />
          )}
        />

        <form.AppField
          name="sort"
          children={(field) => (
            <Select value={field.state.value} onValueChange={field.handleChange}>
              <SelectTrigger className="w-52">
                <SelectValue placeholder="Sort by" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel>Sort by</SelectLabel>
                  <SelectItem value="date">Recently updated</SelectItem>
                  <SelectItem value="date,desc">Least recently updated</SelectItem>
                  <SelectItem value="name">Name ascending</SelectItem>
                  <SelectItem value="name,desc">Name descending</SelectItem>
                  <SelectItem value="state">State</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          )}
        />
      </form>
    </form.AppForm>
  );
}
