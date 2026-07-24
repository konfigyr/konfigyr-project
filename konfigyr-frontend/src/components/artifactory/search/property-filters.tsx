import { useForm, useFormSubmit } from '@konfigyr/components/ui/form';
import { PropertySearchField } from './property-search-field';

import type { PropertySearchQuery } from '@konfigyr/hooks/types';

export function PropertyFilters({ query, onQueryChange }: { query: PropertySearchQuery, onQueryChange: (query: PropertySearchQuery) => void }) {
  const form = useForm({
    defaultValues: {
      term: query.term || '',
    },
    listeners: {
      onChangeDebounceMs: 200,
      onChange: ({ formApi }) => formApi.handleSubmit(),
    },
    onSubmit: ({ value }) => onQueryChange({
      term: value.term ? value.term : '',
    }),
  });

  const onSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form name="property-search-filters" className="flex gap-4 grow" onSubmit={onSubmit}>
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
                <PropertySearchField
                  debounce={0}
                  term={field.state.value}
                  onTermChange={field.handleChange}
                  onBlur={field.handleBlur}
                />
              }
            />
          )}
        />
      </form>
    </form.AppForm>
  );
}
