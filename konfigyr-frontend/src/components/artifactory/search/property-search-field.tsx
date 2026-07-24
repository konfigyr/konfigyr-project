import { useCallback } from 'react';
import { FormattedDate, useIntl } from 'react-intl';
import { SearchIcon, TagIcon } from 'lucide-react';
import { useDebouncedCallback } from 'use-debounce';
import { InputGroup, InputGroupAddon, InputGroupInput } from '@konfigyr/components/ui/input-group';

import type { ChangeEvent, ComponentProps } from 'react';

export type PropertySearchFieldProps = {
  term: string;
  onTermChange: (term: string) => void;
  onBlur?: () => void;
  label?: string;
  placeholder?: string;
  debounce?: number;
} & ComponentProps<typeof InputGroup>;

export function PropertySearchField({
  term,
  label,
  placeholder,
  debounce = 260,
  onTermChange,
  ...props
}: PropertySearchFieldProps) {
  const intl = useIntl();

  const debounced = useDebouncedCallback(
    (value: string) => onTermChange(value),
    debounce,
  );

  const onChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => debounced(event.target.value),
    [debounced],
  );

  return (
    <InputGroup {...props}>
      <InputGroupInput
        type="search"
        defaultValue={term}
        placeholder={placeholder ?? intl.formatMessage({
          defaultMessage: 'Search properties...',
          description: 'Placeholder for the configuration property search input.',
        })}
        aria-label={label ?? intl.formatMessage({
          defaultMessage: 'Search for configuration property metadata',
          description: 'Aria label for the configuration property search input.',
        })}
        onChange={onChange}
      />
      <InputGroupAddon>
        <SearchIcon size="1rem" className="text-muted-foreground"/>
      </InputGroupAddon>
    </InputGroup>
  );
}
