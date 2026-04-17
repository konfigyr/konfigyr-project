import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import {
  ProfilePolicyDescription,
  ProfilePolicyLabel,
} from './messages';

import type { Profile } from '@konfigyr/hooks/vault/types';
import type { ComponentProps } from 'react';

export function PolicyTooltip({ profile, side, children, ...props }: {
  profile: Profile;
  side?: ComponentProps<typeof TooltipContent>['side'];
} & ComponentProps<typeof TooltipTrigger>) {
  return (
    <Tooltip>
      <TooltipTrigger {...props}>
        {children}
      </TooltipTrigger>
      <TooltipContent className="flex-col items-start gap-1" side={side}>
        <p className="font-medium font-heading">
          <ProfilePolicyLabel value={profile.policy} />
        </p>
        <p>
          <ProfilePolicyDescription value={profile.policy} />
        </p>
      </TooltipContent>
    </Tooltip>
  );
}
