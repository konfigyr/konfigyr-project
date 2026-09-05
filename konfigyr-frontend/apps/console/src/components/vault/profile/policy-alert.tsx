import {
  LockIcon,
  ShieldCheckIcon,
  ShieldOffIcon,
} from 'lucide-react';
import { SimpleAlert } from '@konfigyr/components/ui/alert';
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

export function PolicyAlert({ profile, ...props }: { profile: Profile } & ComponentProps<typeof SimpleAlert>) {
  return (
    <SimpleAlert
      icon={
        <PolicyAlertIcon policy={profile.policy} />
      }
      title={
        <ProfilePolicyLabel value={profile.policy} />
      }
      description={
        <ProfilePolicyDescription value={profile.policy} />
      }
      {...props}
    />
  );
}
