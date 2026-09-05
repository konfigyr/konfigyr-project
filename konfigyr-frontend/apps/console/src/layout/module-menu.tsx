import {
  BoxIcon,
  FolderKeyIcon,
  GlobeLockIcon,
  HomeIcon,
  VaultIcon,
} from 'lucide-react';
import { Link } from '@tanstack/react-router';
import {
  ArtifactoryModuleLabel,
  KeyManagementSystemModuleLabel,
  NamespaceOverviewModuleLabel,
  PublicKeyInfrastructureModuleLabel,
  VaultModuleLabel,
} from '@konfigyr/components/messages/modules';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';
import { useModule } from './module';

import type { ReactNode } from 'react';
import type { LucideComponent } from 'lucide-react';
import type { FileRouteTypes } from '@konfigyr/routeTree.gen';
import type { ModuleType } from '@konfigyr/types';
import type { Namespace } from '@konfigyr/hooks/namespace/types';

interface ModuleOption {
  id: ModuleType | 'pki';
  to: FileRouteTypes['to'] | string;
  icon: typeof LucideComponent;
  label: ReactNode;
  disabled?: boolean;
}

const MODULE_OPTIONS: Array<ModuleOption> = [{
  id: 'overview',
  to: '/namespace/$namespace',
  icon: HomeIcon,
  label: <NamespaceOverviewModuleLabel />,
}, {
  id: 'services',
  to: '/namespace/$namespace/services',
  icon: VaultIcon,
  label: <VaultModuleLabel />,
}, {
  id: 'artifactory',
  to: '/namespace/$namespace/artifactory/registry',
  icon: BoxIcon,
  label: <ArtifactoryModuleLabel />,
}, {
  id: 'kms',
  to: '/namespace/$namespace/kms',
  icon: FolderKeyIcon,
  label: <KeyManagementSystemModuleLabel />,
}, {
  id: 'pki',
  to: '/namespace/$namespace',
  icon: GlobeLockIcon,
  label: <PublicKeyInfrastructureModuleLabel />,
  disabled: true,
}];

export function ModuleSidebarMenu({ namespace }: { namespace: Namespace, className?: string }) {
  const module = useModule();

  return (
    <SidebarGroup>
      <SidebarGroupContent className="px-1.5 md:px-0">
        <SidebarMenu>
          {MODULE_OPTIONS.map(item => (
            <SidebarMenuItem key={item.id}>
              <SidebarMenuButton
                disabled={item.disabled}
                aria-disabled={item.disabled}
                tooltip={{
                  children: item.label,
                  hidden: false,
                  sideOffset: 12,
                }}
                render={(
                  <Link
                    to={item.to}
                    params={{ namespace: namespace.slug }}
                    data-active={item.id === module || undefined}
                  >
                    <item.icon/>
                    <span className="sr-only">
                      {item.label}
                    </span>
                  </Link>
                )}
              />
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
