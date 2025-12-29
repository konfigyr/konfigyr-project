import { FolderKeyIcon } from 'lucide-react';
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

export function NamespaceKmsNavigationMenu({ namespace }: { namespace: Namespace}) {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <FolderKeyIcon /> KMS
        </SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild>
              <Link
                to="/namespace/$namespace/kms"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <FormattedMessage
                  defaultMessage="Key management"
                  description="Label for the KMS key management page"
                />
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
