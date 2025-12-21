import { FolderKeyIcon } from 'lucide-react';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';

export function NamespaceKmsNavigationMenu() {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <FolderKeyIcon /> KMS
        </SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton disabled>
              <span className="truncate">Key management</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
