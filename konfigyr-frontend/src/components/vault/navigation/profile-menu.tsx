import { Slot } from 'radix-ui';
import { PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Link  } from '@tanstack/react-router';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';

import type { ReactNode } from 'react';
import type { Namespace, Profile, Service } from '@konfigyr/hooks/types';

function ProfileMenuItem({ children }: { children: ReactNode }) {
  return (
    <Slot.Root
      role="tab"
      className={cn(
        'inline-flex items-center justify-center gap-1.5 px-2 py-1 text-sm font-medium whitespace-nowrap transition',
        'bg-background text-foreground/80 hover:text-foreground dark:text-muted-foreground dark:hover:text-foreground',
        'data-[state=active]:text-foreground dark:data-[state=active]:text-foreground',
        'data-[state=active]:border-primary border-b-2 border-transparent',
        'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:outline-ring focus-visible:ring-[3px] focus-visible:outline-1',
      )}
    >
      {children}
    </Slot.Root>
  );
}

export function ProfileMenu({ namespace, service, profiles }: {
  namespace: Namespace,
  profiles: Array<Profile>,
  service: Service
}) {
  return (
    <div
      role="tablist"
      data-slot="profile-menu-tabs"
      className="text-muted-foreground flex w-full gap-2 bg-background border-b"
    >
      {profiles.sort((a, b) => a.position - b.position).map((profile) => (
        <ProfileMenuItem key={profile.id}>
          <Link
            to="/namespace/$namespace/services/$service/profiles/$profile"
            params={{ namespace: namespace.slug, service: service.slug, profile: profile.slug }}
            activeProps={{ 'data-state': 'active' }}
            activeOptions={{ exact: true }}
          >
            {profile.name}
          </Link>
        </ProfileMenuItem>
      ))}

      <ProfileMenuItem>
        <Tooltip>
          <TooltipTrigger asChild>
            <Link
              to="/namespace/$namespace/services/$service/create-profile"
              params={{ namespace: namespace.slug, service: service.slug }}
              className="inline-flex items-center px-2 rounded-sm hover:bg-accent [&_svg]:shrink-0 [&_svg]:size-4"
            >
              <PlusIcon />
            </Link>
          </TooltipTrigger>
          <TooltipContent>
            <FormattedMessage
              defaultMessage="Create new configuration profile"
              description="Tooltip content for creating a new configuration profile."
            />
          </TooltipContent>
        </Tooltip>
      </ProfileMenuItem>
    </div>
  );
}
