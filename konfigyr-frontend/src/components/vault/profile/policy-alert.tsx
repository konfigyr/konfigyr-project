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
import type { Profile, ProfilePolicy } from '@konfigyr/hooks/types';

import type { ComponentProps } from 'react';

export function PolicyAlertIcon({ policy }: { policy: ProfilePolicy }) {
  switch (policy) {
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
      <PolicyAlertIcon policy={profile.policy} />
      <AlertTitle>
        <ProfilePolicyLabel value={profile.policy} />
      </AlertTitle>
      <AlertDescription>
        <ProfilePolicyDescription value={profile.policy} />
      </AlertDescription>
    </Alert>
  );
}
