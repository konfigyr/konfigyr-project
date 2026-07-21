import { PackageIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';
import { Link } from '@tanstack/react-router';

import type { Namespace } from '@konfigyr/hooks/types';

export function NamespaceArtifactoryNavigationMenu({ namespace }: { namespace: Namespace }) {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <PackageIcon /> Artifactory
        </SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton render={
              <Link
                to="/namespace/$namespace/artifactory/groups"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <FormattedMessage
                  defaultMessage="Group verifications"
                  description="Label for the Group verifications page"
                />
              </Link>
            } />
            <SidebarMenuButton disabled>
              <span className="truncate">Ownership transfers</span>
            </SidebarMenuButton>
            <SidebarMenuButton disabled>
              <span className="truncate">Artifact registry</span>
            </SidebarMenuButton>
            <SidebarMenuButton disabled>
              <span className="truncate">Property search</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
