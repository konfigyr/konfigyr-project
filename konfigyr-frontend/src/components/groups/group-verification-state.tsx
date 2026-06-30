import { FormattedMessage } from 'react-intl';
import { Badge } from '@konfigyr/components/ui/badge';

import type { ComponentProps } from 'react';
import type { VerificationState } from '@konfigyr/hooks/types';

export function GroupVerificationState({ state }: { state?: string | VerificationState }) {
  switch (state) {
    case 'ACTIVE':
      return <FormattedMessage
        defaultMessage="Active"
        description="Name or label of the ACTIVE group verification state."
      />;
    case 'PENDING':
      return <FormattedMessage
        defaultMessage="Pending"
        description="Name or label of the PENDING group verification state."
      />;
    case 'FAILED':
      return <FormattedMessage
        defaultMessage="Failed"
        description="Name or label of the FAILED group verification state."
      />;
    case 'REVOKED':
      return <FormattedMessage
        defaultMessage="Revoked"
        description="Name or label of the REVOKED group verification state."
      />;
    default:
      return null;
  }
}

export function GroupVerificationStateBadge({ state, ...props }: {
  state?: string | VerificationState;
} & Omit<ComponentProps<typeof Badge>, 'variant'>) {
  const label = <GroupVerificationState state={state} />;
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
    case 'PENDING':
      return (
        <Badge variant="warning" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'FAILED':
      return (
        <Badge variant="destructive" {...props}>
          {icon} {label}
        </Badge>
      );
    case 'REVOKED':
      return (
        <Badge variant="secondary" {...props}>
          {icon} {label}
        </Badge>
      );
  }
}
