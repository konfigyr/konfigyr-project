import {
  LockIcon,
  ShieldCheckIcon,
  ShieldOffIcon,
} from 'lucide-react';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@konfigyr/components/ui/alert';
import {
  ProfilePolicyDescription,
  ProfilePolicyLabel,
} from './messages';

import type { ComponentProps } from 'react';
import type { Profile } from '@konfigyr/hooks/types';

function PolicyAlertIcon({ profile }: { profile: Profile }) {
  switch (profile.policy) {
    case 'IMMUTABLE':
      return <LockIcon />;
    case 'PROTECTED':
      return <ShieldCheckIcon />;
    case 'UNPROTECTED':
      return <ShieldOffIcon />;
  }
}

export function PolicyAlert({ profile, ...props }: { profile: Profile } & ComponentProps<typeof Alert>) {
  return (
    <Alert {...props}>
      <PolicyAlertIcon profile={profile} />
      <AlertTitle>
        <ProfilePolicyLabel value={profile.policy} />
      </AlertTitle>
      <AlertDescription>
        <ProfilePolicyDescription value={profile.policy} />
      </AlertDescription>
    </Alert>
  );
}
