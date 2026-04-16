import { useCallback } from 'react';
import { useIntl } from 'react-intl';
import { useGetProfiles } from '@konfigyr/hooks/vault/profiles';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
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
import { ChangeRequestStateLabel } from './messages';

import type { IntlShape } from 'react-intl';
import type { ChangeRequestQuery, Namespace, Service } from '@konfigyr/hooks/types';

enum SortBy {
  LATEST = 'date,desc',
  OLDEST = 'date',
  RECENTLY_UPDATED = 'updated,desc',
  LEAST_RECENTLY_UPDATED = 'updated',
}

const useSortByLabel = (intl: IntlShape) => useCallback(
  (value: SortBy) => {
    switch(value) {
      case SortBy.LATEST:
        return intl.formatMessage({
          defaultMessage: 'Latest',
          description: 'Label for the latest sort option. This should force the page to load the latest resources first from the server.',
        });
      case SortBy.OLDEST:
        return intl.formatMessage({
          defaultMessage: 'Oldest',
          description: 'Label for the oldest sort option. This should force the page to load the oldest resources first from the server.',
        });
      case SortBy.RECENTLY_UPDATED:
        return intl.formatMessage({
          defaultMessage: 'Recently updated',
          description: 'Label for the most recently updated option. This should force the page to load the most recently updated resources first from the server.',
        });
      case SortBy.LEAST_RECENTLY_UPDATED:
        return intl.formatMessage({
          defaultMessage: 'Least recently updated',
          description: 'Label for the least recently updated option. This should force the page to load the least recently updated resources first from the server.',
        });
    }
  },
  [intl],
);

export function ChangeRequestFilters({ namespace, service, query, onQueryChange }: {
  namespace: Namespace,
  service: Service,
  query: ChangeRequestQuery;
  onQueryChange: (query: ChangeRequestQuery) => void;
}) {
  const intl = useIntl();
  const sortByLabel = useSortByLabel(intl);
  const { data } = useGetProfiles(namespace, service);

  const form = useForm({
    defaultValues: {
      term: query.term || '',
      profile: query.profile || '',
      state: query.state || ChangeRequestState.OPEN,
      sort: query.sort || SortBy.LATEST,
    },
    listeners: {
      onChangeDebounceMs: 200,
      onChange: ({ formApi }) => formApi.handleSubmit(),
    },
    onSubmit: ({ value }) => onQueryChange({
      profile: value.profile ? value.profile : undefined,
      term: value.term ? value.term : undefined,
      sort: value.sort,
      state: value.state,
    }),
  });

  const onSubmit = useFormSubmit(form);

  const filterByStateLabel = intl.formatMessage({
    defaultMessage: 'Filter by state',
    description: 'Label for the change request filter dropdown by filter by change request state.',
  });
  const filterByProfileLabel = intl.formatMessage({
    defaultMessage: 'Filter by profile',
    description: 'Label for the change request filter dropdown by filter by target profile.',
  });
  const sortLabel = intl.formatMessage({
    defaultMessage: 'Sort by',
    description: 'Label used by the sort by filter dropdown.',
  });

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
                <field.SearchInput
                  placeholder={intl.formatMessage({
                    defaultMessage: 'Search change requests...',
                    description: 'Placeholder text for the search input in the change request list page.',
                  })}
                  aria-label={intl.formatMessage({
                    defaultMessage: 'Search change requests',
                    description: 'Label for the search input in the change request list page.',
                  })}
                  className="text-sm"
                />
              }
            />
          )}
        />

        <form.AppField
          name="state"
          children={(field) => (
            <Select
              value={field.state.value}
              onValueChange={it => field.handleChange(it || ChangeRequestState.OPEN)}
            >
              <SelectTrigger className="w-52" aria-label={filterByStateLabel}>
                <SelectValue>
                  <ChangeRequestStateLabel value={field.state.value} />
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel>
                    {filterByStateLabel}
                  </SelectLabel>

                  {Object.values(ChangeRequestState).map(option => (
                    <SelectItem key={option} value={option}>
                      <ChangeRequestStateLabel value={option} />
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          )}
        />

        <form.AppField
          name="profile"
          children={(field) => (
            <Select
              value={field.state.value}
              onValueChange={it => field.handleChange(it || '')}
            >
              <SelectTrigger className="w-52" aria-label={filterByProfileLabel}>
                <SelectValue placeholder="Profile" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel reset={true} onReset={() => field.handleChange('')}>
                    {filterByProfileLabel}
                  </SelectLabel>
                  {data?.map(profile => (
                    <SelectItem key={profile.id} value={profile.slug}>{profile.name}</SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          )}
        />

        <form.AppField
          name="sort"
          children={(field) => (
            <Select
              value={field.state.value as string}
              onValueChange={it => field.handleChange(it || SortBy.LATEST)}
            >
              <SelectTrigger className="w-52" aria-label={sortLabel}>
                <SelectValue>
                  {sortByLabel(field.state.value as SortBy.LATEST)}
                </SelectValue>
              </SelectTrigger>
              <SelectContent className="min-w-56">
                <SelectGroup>
                  <SelectLabel>
                    {sortLabel}
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
