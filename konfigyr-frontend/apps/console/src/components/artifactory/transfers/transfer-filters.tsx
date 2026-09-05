import { useCallback } from 'react';
import { useIntl } from 'react-intl';
import { SearchIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import { InputGroup, InputGroupAddon, InputGroupInput } from '@konfigyr/components/ui/input-group';

import type { ChangeEvent } from 'react';
import type { ArtifactOwnershipTransferQuery } from '@konfigyr/hooks/types';

export function TransferFilters({ query, onQueryChange }: {
  query: ArtifactOwnershipTransferQuery,
  onQueryChange: (query: ArtifactOwnershipTransferQuery) => void,
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
          defaultMessage: 'Search ownership transfers...',
          description: 'Placeholder text for the ownership transfers search input.',
        })}
        aria-label={intl.formatMessage({
          defaultMessage: 'Search ownership transfers',
          description: 'Label for the ownership transfers search input.',
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
