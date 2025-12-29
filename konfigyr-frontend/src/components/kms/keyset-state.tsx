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

import type { ReactNode } from 'react';
import type { KeysetState } from '@konfigyr/hooks/types';

const STATES: Array<KeysetState> = [
  'ACTIVE',
  'INACTIVE',
  'PENDING_DESTRUCTION',
  'DESTROYED',
];

export function KeysetState({ state }: { state?: string | KeysetState }) {
  switch (state) {
    case 'ACTIVE':
      return <FormattedMessage
        defaultMessage="Active"
        description="Name or label of the ACTIVE keyset state."
      />;
    case 'INACTIVE':
      return <FormattedMessage
        defaultMessage="Inactive"
        description="Name or label of the INACTIVE keyset state."
      />;
    case 'PENDING_DESTRUCTION':
      return <FormattedMessage
        defaultMessage="Disabled"
        description="Name or label of the PENDING_DESTRUCTION keyset state."
      />;
    case 'DESTROYED':
      return <FormattedMessage
        defaultMessage="Destroyed"
        description="Name or label of the DESTROYED keyset state."
      />;
    default:
      return null;
  }
}

export function KeysetStateSelect({ value, reset = false, placeholder, className, onReset, onChange }: {
  value?: string | KeysetState,
  reset?: boolean,
  placeholder?: string | ReactNode,
  className?: string,
  onChange?: (value: string | KeysetState) => void,
  onReset?: () => void
}) {
  return (
    <Select name="keyset-state" value={value} onValueChange={onChange}>
      <SelectTrigger className={className}>
        <SelectValue placeholder={placeholder}>
          <KeysetState state={value} />
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel reset={reset} onReset={onReset}>
            <FormattedMessage
              defaultMessage="States"
              description="Label for the KMS keyset state select dropdown menu."
            />
          </SelectLabel>
          {STATES.map((state) => (
            <SelectItem key={state} value={state}>
              <KeysetState state={state} />
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
