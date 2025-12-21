import { PlusIcon, ServerIcon, ServerOffIcon } from 'lucide-react';
import { Button } from '@konfigyr/components/ui/button';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
} from '@konfigyr/components/ui/sidebar';

export function NamespaceServicesNavigationMenu() {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <ServerIcon />
          <span className="flex-grow">Services</span>
          <Button variant="ghost" size="sm">
            <PlusIcon size="1rem"/>
          </Button>
        </SidebarGroupLabel>
        <SidebarMenu>
          <EmptyState
            title="No services found"
            description="There are currently no services for this namespace. Why don't you create one?"
            icon={<ServerOffIcon />}
            size="sm"
          />
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
