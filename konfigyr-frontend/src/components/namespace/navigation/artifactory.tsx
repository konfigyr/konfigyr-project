import { PackageIcon } from 'lucide-react';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';
import { Link } from '@tanstack/react-router';
import { GroupClaimsLabel } from '@konfigyr/components/artifactory/groups/messages';
import { RegistryLabel } from '@konfigyr/components/artifactory/registry/messages';
import { PropertySearchLabel } from '@konfigyr/components/artifactory/search/messages';
import { TransfersLabel } from '@konfigyr/components/artifactory/transfers/messages';

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
                to="/namespace/$namespace/artifactory/registry"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <RegistryLabel />
              </Link>
            } />
          </SidebarMenuItem>
          <SidebarMenuItem>
            <SidebarMenuButton render={
              <Link
                to="/namespace/$namespace/artifactory/search"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <PropertySearchLabel />
              </Link>
            } />
          </SidebarMenuItem>
          <SidebarMenuItem>
            <SidebarMenuButton render={
              <Link
                to="/namespace/$namespace/artifactory/groups"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <GroupClaimsLabel />
              </Link>
            } />
          </SidebarMenuItem>
          <SidebarMenuItem>
            <SidebarMenuButton render={
              <Link
                to="/namespace/$namespace/artifactory/transfers"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <TransfersLabel />
              </Link>
            } />
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
