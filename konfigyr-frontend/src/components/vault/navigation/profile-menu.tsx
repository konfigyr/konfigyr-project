import { PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { TabItem, Tabs } from '@konfigyr/components/ui/tab';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { Namespace, Profile, Service } from '@konfigyr/hooks/types';

export function ProfileMenu({ namespace, service, profiles }: {
  namespace: Namespace,
  profiles: Array<Profile>,
  service: Service
}) {
  return (
    <Tabs>
      {profiles.sort((a, b) => a.position - b.position).map((profile) => (
        <TabItem key={profile.id}>
          <Link
            to="/namespace/$namespace/services/$service/profiles/$profile"
            params={{ namespace: namespace.slug, service: service.slug, profile: profile.slug }}
            activeProps={{ 'data-state': 'active' }}
            activeOptions={{ exact: true }}
          >
            {profile.name}
          </Link>
        </TabItem>
      ))}

      <TabItem>
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
      </TabItem>
    </Tabs>
  );
}
