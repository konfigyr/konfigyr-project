import { GlobeLockIcon } from 'lucide-react';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';

export function NamespacePkiNavigationMenu() {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <GlobeLockIcon /> PKI
        </SidebarGroupLabel>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton disabled>
              <span className="truncate">Certificates</span>
            </SidebarMenuButton>
            <SidebarMenuButton disabled>
              <span className="truncate">Authorities</span>
            </SidebarMenuButton>
            <SidebarMenuButton disabled>
              <span className="truncate">Alerts</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
