import { FormattedMessage } from 'react-intl';
import { Badge } from '@konfigyr/components/ui/badge';

import type { ComponentProps } from 'react';
import type { TransferState } from '@konfigyr/hooks/types';

export function TransferStateLabel ({ state }: { state?: string | TransferState }) {
  switch (state) {
    case 'PENDING':
      return <FormattedMessage
        defaultMessage="Pending"
        description="Name or label of the PENDING ownership transfer state."
      />;
    case 'ACCEPTED':
      return <FormattedMessage
        defaultMessage="Accepted"
        description="Name or label of the ACCEPTED ownership transfer state."
      />;
    case 'REJECTED':
      return <FormattedMessage
        defaultMessage="Rejected"
        description="Name or label of the REJECTED ownership transfer state."
      />;
    case 'CANCELLED':
      return <FormattedMessage
        defaultMessage="Cancelled"
        description="Name or label of the CANCELLED ownership transfer state."
      />;
    default:
      return null;
  }
}

export function TransferStateBadge ({ state, ...props }: {
  state?: string | TransferState;
} & Omit<ComponentProps<typeof Badge>, 'variant'>) {
  const label = <TransferStateLabel state={state}/>;
  const icon = (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 4 4" width="4" height="4">
      <circle cx="2" cy="2" r="1" fill="currentColor"/>
    </svg>
  );

  switch (state) {
    case 'PENDING':
      return (
        <Badge variant="warning" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'ACCEPTED':
      return (
        <Badge variant="success" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'REJECTED':
      return (
        <Badge variant="destructive" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'CANCELLED':
      return (
        <Badge variant="secondary" {...props}>
          {icon} {label}
        </Badge>
      );
  }
}
