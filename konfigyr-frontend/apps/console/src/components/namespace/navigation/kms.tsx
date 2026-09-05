import { FolderKeyIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  KeyManagementSystemModuleLabel,
} from '@konfigyr/components/messages/modules';
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

export function NamespaceKmsNavigationMenu({ namespace }: { namespace: Namespace }) {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel>
          <KeyManagementSystemModuleLabel />
        </SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton render={
              <Link
                to="/namespace/$namespace/kms"
                params={{ namespace: namespace.slug }}
                className="truncate"
                activeProps={{ 'data-active': true }}
              >
                <FolderKeyIcon />
                <FormattedMessage
                  defaultMessage="Keysets"
                  description="Label for the KMS keyset list page"
                />
              </Link>
            } />
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
