import { FormattedMessage } from 'react-intl';
import {
  SortByLabel,
  SortByLeastRecentlyUpdated,
  SortByMostRecentlyUpdated,
  SortByNameAscending,
  SortByNameDescending,
} from '@konfigyr/components/messages/sort';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';
import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { KeysetAlgorithmSelect } from './keyset-algorithm';
import { KeysetStateSelect } from './keyset-state';

import type { KeysetSearchQuery, KeysetState } from '@konfigyr/hooks/types';

enum SortBy {
  MOST_RECENTLY_UPDATED = 'date',
  LEAST_RECENTLY_UPDATED = 'date,desc',
  NAME_ASCENDING = 'name',
  NAME_DESCENDING = 'name,desc',
}

const sortByLabel = (value: SortBy) => {
  switch(value) {
    case SortBy.LEAST_RECENTLY_UPDATED:
      return <SortByLeastRecentlyUpdated />;
    case SortBy.MOST_RECENTLY_UPDATED:
      return <SortByMostRecentlyUpdated />;
    case SortBy.NAME_ASCENDING:
      return <SortByNameAscending />;
    case SortBy.NAME_DESCENDING:
      return <SortByNameDescending />;
  }
};

export function KeysetFilters({ query, onQueryChange }: { query: KeysetSearchQuery, onQueryChange: (query: KeysetSearchQuery) => void }) {
  const form = useForm({
    defaultValues: {
      term: query.term || '',
      state: query.state || '',
      algorithm: query.algorithm || '',
      sort: query.sort || SortBy.MOST_RECENTLY_UPDATED,
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

  const onSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form name="keyset-filters" className="flex gap-4 grow" onSubmit={onSubmit}>
        <form.AppField
          name="term"
          listeners={{
            onBlurDebounceMs: 200,
            onChangeDebounceMs: 200,
          }}
          children={(field) => (
            <field.Control
              className="grow"
              render={
                <field.Input
                  type="search"
                  placeholder="Search keysets..."
                  aria-label="Search keysets"
                />
              }
            />
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
              onChange={it => field.handleChange(it || '')}
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
              onChange={it => field.handleChange(it || '')}
              onReset={() => field.handleChange('')}
            />
          )}
        />

        <form.AppField
          name="sort"
          children={(field) => (
            <Select value={field.state.value} onValueChange={it => field.handleChange(it || '')}>
              <SelectTrigger className="w-52">
                <SelectValue>
                  {sortByLabel(field.state.value as SortBy)}
                </SelectValue>
              </SelectTrigger>
              <SelectContent className="min-w-52">
                <SelectGroup>
                  <SelectLabel>
                    <SortByLabel />
                  </SelectLabel>

                  {Object.values(SortBy).map(value => (
                    <SelectItem key={value} value={value}>
                      {sortByLabel(value)}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          )}
        />
      </form>
    </form.AppForm>
  );
}
