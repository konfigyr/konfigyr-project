import { useCallback, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  BoxIcon,
  FolderKeyIcon,
  GlobeLockIcon,
  HomeIcon,
  LayoutGridIcon,
  UserShieldIcon,
  VaultIcon,
} from 'lucide-react';
import { useMatches, useRouter } from '@tanstack/react-router';
import { Badge } from '@konfigyr/components/ui/badge';
import { Button } from '@konfigyr/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { cn } from '@konfigyr/components/utils';

import type { ReactNode } from 'react';
import type { LucideComponent } from 'lucide-react';
import type { FileRouteTypes, FileRoutesById } from '@konfigyr/routeTree.gen';
import type { Namespace } from '@konfigyr/hooks/namespace/types';

interface Module {
  id: FileRouteTypes['id'] | string;
  icon: typeof LucideComponent;
  label: ReactNode;
  exact?: boolean;
  disabled?: boolean;
}

const MODULES: Array<Module> = [{
  id: '/_authenticated/namespace/$namespace/',
  icon: HomeIcon,
  label: <FormattedMessage defaultMessage="Home" description="Label for the home module switcher option" />,
  exact: true,
}, {
  id: '/_authenticated/namespace/$namespace/services/$service',
  icon: VaultIcon,
  label: <FormattedMessage defaultMessage="Vault" description="Label for the vault module switcher option" />,
}, {
  id: '/_authenticated/namespace/$namespace/artifactory/registry/',
  icon: BoxIcon,
  label: <FormattedMessage defaultMessage="Artifactory" description="Label for the artifactory module switcher option" />,
}, {
  id: '/_authenticated/namespace/$namespace/kms',
  icon: FolderKeyIcon,
  label: <FormattedMessage defaultMessage="KMS" description="Label for the KMS module switcher option" />,
}, {
  id: '/_authenticated/namespace/$namespace/pki',
  icon: GlobeLockIcon,
  label: <FormattedMessage defaultMessage="PKI" description="Label for the PKI module switcher option" />,
  disabled: true,
}, {
  id: '/_authenticated/namespace/$namespace/settings/',
  icon: UserShieldIcon,
  label: <FormattedMessage defaultMessage="Administration" description="Label for the administration module switcher option" />,
}];

function ModuleLabel({ module, variant = 'default' }: { module: Module, variant?: 'menuitem' | 'default' }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={cn(
          variant === 'menuitem' ? 'size-6' : 'size-8',
          'bg-accent text-accent-foreground size-8 flex justify-center items-center rounded-md',
        )}
      >
        <module.icon className="size-4" />
      </span>
      <span className={cn(variant === 'default' && 'font-medium text-lg grow capitalize')}>
        {module.label}
      </span>
      {(module.disabled && variant === 'menuitem') && (
        <Badge size="xs">
          <FormattedMessage defaultMessage="Coming soon" description="Label for coming soon module switcher option" />
        </Badge>
      )}
    </div>
  );
}

export function ModuleSwitcher({ namespace, className }: { namespace: Namespace, className?: string }) {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const matches = useMatches();

  const matching = useMemo(() => {
    return MODULES.find(module => matches.find(match => {
      if (module.exact) {
        return match.routeId === module.id;
      }
      return match.routeId.startsWith(module.id);
    }));
  }, [matches]);

  const onSelect = useCallback((value: string) => {
    const selected = router.routesById[value as keyof FileRoutesById];
    router.navigate({ to: selected.to, params: { namespace: namespace.slug } });

    setOpen(false);
  }, [namespace]);

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <div className={cn('flex items-center gap-2', className)}>
        <DropdownMenuTrigger render={
          <Button variant="ghost" size="icon-lg" className="rounded-md">
            <LayoutGridIcon />
            <span className="sr-only">
              <FormattedMessage
                defaultMessage="Switch domain"
                description="Accessability label used by the module swticher component."
              />
            </span>
          </Button>
        }/>
        {matching && (
          <ModuleLabel module={matching} />
        )}
      </div>
      <DropdownMenuContent className="p-2 min-w-64 font-medium text-xl">
        <DropdownMenuGroup>
          <DropdownMenuRadioGroup onValueChange={onSelect} value={matching?.id ?? ''}>
            {MODULES.map(module => (
              <DropdownMenuRadioItem
                key={module.id}
                value={module.id}
                disabled={module.disabled}
                aria-disabled={module.disabled}
                className="py-1"
              >
                <ModuleLabel module={module} variant="menuitem" />
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
