import {
  LockIcon,
  ShieldCheckIcon,
  ShieldOffIcon,
} from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@konfigyr/components/ui/alert';

import type { ComponentProps } from 'react';
import type { Profile } from '@konfigyr/hooks/types';

function ImmutableProfileAlert(props: ComponentProps<typeof Alert>) {
  return (
    <Alert {...props}>
      <LockIcon />
      <AlertTitle>
        <FormattedMessage
          defaultMessage="Profile is read-only"
          description="Alert title for immutable profile"
        />
      </AlertTitle>
      <AlertDescription>
        <FormattedMessage
          defaultMessage="No configuration changes may be applied to this profile. The existing configuration remains readable and auditable but cannot be modified."
          description="Alert description for immutable profile, stating that profile is locked and cannot be edited."
        />
      </AlertDescription>
    </Alert>
  );
}

function ProtectedProfileAlert(props: ComponentProps<typeof Alert>) {
  return (
    <Alert {...props}>
      <ShieldCheckIcon />
      <AlertTitle>
        <FormattedMessage
          defaultMessage="Protected profile"
          description="Alert title for protected profile"
        />
      </AlertTitle>
      <AlertDescription>
        <FormattedMessage
          defaultMessage="Changes to this profile require review and approval before being applied."
          description="Alert description for protected profile, stating that changes requires review and approval before being applied."
        />
      </AlertDescription>
    </Alert>
  );
}

function UnprotectedProfileAlert(props: ComponentProps<typeof Alert>) {
  return (
    <Alert {...props}>
      <ShieldOffIcon />
      <AlertTitle>
        <FormattedMessage
          defaultMessage="Unprotected profile"
          description="Alert title for unprotected profile"
        />
      </AlertTitle>
      <AlertDescription>
        <FormattedMessage
          defaultMessage="Changes to this profile will be directly applied without any review or approval."
          description="Alert description for unprotected profile, stating that changes can be directly applied."
        />
      </AlertDescription>
    </Alert>
  );
}

export function PolicyAlert({ profile, ...props }: { profile: Profile } & ComponentProps<typeof Alert>) {
  switch (profile.policy) {
    case 'IMMUTABLE':
      return <ImmutableProfileAlert {...props}/>;
    case 'PROTECTED':
      return <ProtectedProfileAlert {...props}/>;
    case 'UNPROTECTED':
      return <UnprotectedProfileAlert {...props}/>;
    default:
      return null;
  }
}
