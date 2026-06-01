import { FormattedMessage } from 'react-intl';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';

import type { ComponentProps, ReactNode } from 'react';
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

export function KeysetStateBadge({ state, ...props }: {
  state?: string | KeysetState;
} & Omit<ComponentProps<typeof Badge>, 'variant'>) {
  const label = <KeysetState state={state} />;
  const icon = (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 4 4" width="4" height="4">
      <circle cx="2" cy="2" r="1" fill="currentColor" />
    </svg>
  );

  switch (state) {
    case 'ACTIVE':
      return (
        <Badge variant="success" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'INACTIVE':
      return (
        <Badge variant="outline" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'PENDING_DESTRUCTION':
    case 'DESTROYED':
      return (
        <Badge variant="destructive" {...props}>
          {icon} {label}
        </Badge>
      );
  }
}

export function KeysetStateSelect({ value, reset = false, placeholder, className, onReset, onChange }: {
  value?: string | KeysetState,
  reset?: boolean,
  placeholder?: string | ReactNode,
  className?: string,
  onChange?: (value: string | KeysetState | null) => void,
  onReset?: () => void
}) {
  return (
    <Select name="keyset-state" value={value} onValueChange={onChange}>
      <SelectTrigger className={className}>
        <SelectValue>
          {value ? (<KeysetState state={value} />) : placeholder}
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
