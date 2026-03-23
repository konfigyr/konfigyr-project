import { useCallback } from 'react';
import { useIntl } from 'react-intl';
import { SearchIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import { InputGroup, InputGroupAddon, InputGroupInput } from '@konfigyr/components/ui/input-group';

import type { ChangeEvent, ComponentProps } from 'react';

export function SearchInputGroup({ value, debounce = 200, onChange, ...props }: {
  value: string,
  debounce?: number,
  onChange: (value: string) => void,
} & Omit<ComponentProps<typeof InputGroup>, 'onChange'>) {
  const intl = useIntl();
  const debounced = useDebouncedCallback(onChange, debounce);

  const onChangeEvent = useCallback((event: ChangeEvent<HTMLInputElement>) => {
    debounced(event.target.value);
  }, [debounced]);

  return (
    <InputGroup {...props}>
      <InputGroupInput
        type="search"
        defaultValue={value}
        placeholder={intl.formatMessage({
          defaultMessage: 'Filter properties...',
          description: 'Placeholder text for the search input in the service catalog.',
        })}
        aria-label={intl.formatMessage({
          defaultMessage: 'Search properties',
          description: 'Label for the search input in the service catalog.',
        })}
        className="text-sm"
        onChange={onChangeEvent}
      />
      <InputGroupAddon>
        <SearchIcon size="1rem" className="text-muted-foreground" />
      </InputGroupAddon>
    </InputGroup>
  );
}
