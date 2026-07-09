import { FormattedMessage, useIntl } from 'react-intl';
import { Badge } from '@konfigyr/components/ui/badge';

import { SimpleAlert } from '@konfigyr/components/ui/alert';
import { AlertCircleIcon, CheckCircle2Icon, CircleAlertIcon, Trash2Icon } from 'lucide-react';
import { sourceCodeHostLabel } from '@konfigyr/components/groups/group-verification-method';
import type { ComponentProps } from 'react';
import type { VerificationMethod, VerificationState } from '@konfigyr/hooks/types';

export type GroupVerificationStateAlertProps = {
  groupId: string;
  verificationChallenge?: { method: VerificationMethod; token: string } | undefined;
  verificationState: string;
};

export function GroupVerificationState ({ state }: { state?: string | VerificationState }) {
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

export function GroupVerificationStateBadge ({ state, ...props }: {
  state?: string | VerificationState;
} & Omit<ComponentProps<typeof Badge>, 'variant'>) {
  const label = <GroupVerificationState state={state}/>;
  const icon = (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 4 4" width="4" height="4">
      <circle cx="2" cy="2" r="1" fill="currentColor"/>
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

export function GroupVerificationStateAlert ({
  groupId,
  verificationChallenge,
  verificationState,
}: GroupVerificationStateAlertProps) {
  switch (verificationState) {
    case 'ACTIVE':
      return (
        <ActiveStateAlert groupId={groupId}/>
      );
    case 'PENDING':
      return (
        <PendingStateAlert groupId={groupId} method={verificationChallenge?.method}/>
      );
    case 'FAILED':
      return (
        <FailedStateAlert/>
      );
    case 'REVOKED':
    default:
      return (
        <RevokedAlertAlert groupId={groupId}/>
      );
  }
}

function ActiveStateAlert ({ groupId }: { groupId: string }) {
  return (
    <SimpleAlert
      icon={
        <CheckCircle2Icon className="size-8"/>
      }
      title={
        <FormattedMessage
          defaultMessage="Ownership verified"
          description="Banner title shown when a group claim is active."
        />
      }
      description={
        <FormattedMessage
          defaultMessage="This namespace can publish artifact metadata under {groupId} and its sub-groups."
          values={{ groupId }}
          description="Banner description shown when a group claim is active."
        />
      }
      variant="success"
    />
  );
}

function PendingStateAlert ({ groupId, method }: { groupId: string, method?: VerificationMethod }) {
  const { label: host } = sourceCodeHostLabel(groupId);
  return (
    <SimpleAlert
      icon={
        <AlertCircleIcon className="size-8"/>
      }
      title={
        <FormattedMessage
          defaultMessage="Claim pending"
          description="Banner title shown when a group claim is waiting for verification."
        />
      }
      description={
        method === 'SOURCE_CODE' ? (
          <FormattedMessage
            defaultMessage="Create the marker repository for {groupId} on {host}, then re-check this claim."
            values={{ groupId, host }}
            description="Banner description shown for a pending source-code group claim."
          />
        ) : (
          <FormattedMessage
            defaultMessage="Publishing under {groupId} is blocked until the TXT record is visible in DNS."
            values={{ groupId }}
            description="Banner description shown for a pending DNS group claim."
          />
        )
      }
      variant="warning"
    />
  );
}

function FailedStateAlert () {
  return (
    <SimpleAlert
      icon={
        <CircleAlertIcon className="size-8"/>
      }
      title={
        <FormattedMessage
          defaultMessage="Verification failed"
          description="Banner title shown when a group claim failed verification."
        />
      }
      description={
        <FormattedMessage
          defaultMessage="The claim could not be verified. Fix the record or repository and try again."
          description="Banner description shown when a group claim failed verification."
        />
      }
      variant="destructive"
    />
  );
}

function RevokedAlertAlert ({ groupId }: { groupId: string }) {
  return (
    <SimpleAlert
      icon={
        <Trash2Icon className="size-8"/>
      }
      title={
        <FormattedMessage
          defaultMessage="Claim revoked"
          description="Banner title shown when a group claim is revoked."
        />
      }
      description={
        <FormattedMessage
          defaultMessage="Publishing under {groupId} is blocked. You can claim this groupId again at any time."
          values={{ groupId }}
          description="Banner description shown when a group claim is revoked."
        />
      }
      variant="default"
    />
  );
}