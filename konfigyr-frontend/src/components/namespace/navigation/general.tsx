import {
  MonitorIcon,
  SendIcon,
  Settings2Icon,
  UsersIcon,
} from 'lucide-react';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';
import { Link } from '@tanstack/react-router';

import type { Namespace } from '@konfigyr/hooks/types';

export function NamespaceNavigationMenu({ namespace }: { namespace: Namespace }) {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild>
              <Link
                to="/namespace/$namespace"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeOptions={{ exact: true }}
                activeProps={{ 'data-active': true }}
              >
                <MonitorIcon /> Overview
              </Link>
            </SidebarMenuButton>
            <SidebarMenuButton asChild>
              <Link
                to="/namespace/$namespace/members"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <UsersIcon /> Members
              </Link>
            </SidebarMenuButton>
            <SidebarMenuButton asChild>
              <Link
                to="/namespace/$namespace/invitations"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <SendIcon /> Invitations
              </Link>
            </SidebarMenuButton>
            <SidebarMenuButton asChild>
              <Link
                to="/namespace/$namespace/settings"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <Settings2Icon /> Settings
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
