import { useCallback } from 'react';
import { useIntl } from 'react-intl';
import { SearchIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import { InputGroup, InputGroupAddon, InputGroupInput } from '@konfigyr/components/ui/input-group';

import type { ChangeEvent } from 'react';
import type { ArtifactQuery } from '@konfigyr/hooks/artifactory/types';

export function ArtifactFilters({ query, onQueryChange }: {
  query: ArtifactQuery,
  onQueryChange: (query: ArtifactQuery) => void,
}) {
  const intl = useIntl();
  const debounced = useDebouncedCallback(
    (term: string) => onQueryChange({ term: term || undefined }),
    200,
  );

  const onChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    debounced(event.target.value);
  }, [debounced]);

  return (
    <InputGroup className="grow">
      <InputGroupInput
        type="search"
        defaultValue={query.term || ''}
        placeholder={intl.formatMessage({
          defaultMessage: 'Search artifacts...',
          description: 'Placeholder text for the artifact registry search input.',
        })}
        aria-label={intl.formatMessage({
          defaultMessage: 'Search artifacts',
          description: 'Label for the artifact registry search input.',
        })}
        className="text-sm"
        onChange={onChange}
      />
      <InputGroupAddon>
        <SearchIcon size="1rem" className="text-muted-foreground" />
      </InputGroupAddon>
    </InputGroup>
  );
}
