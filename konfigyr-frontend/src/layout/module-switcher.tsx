import {
  useCallback,
  useMemo,
  useState,
} from 'react';
import { FormattedMessage } from 'react-intl';
import {
  BoxIcon,
  FolderKeyIcon,
  GlobeLockIcon,
  HomeIcon,
  LayoutGridIcon,
  VaultIcon,
} from 'lucide-react';
import { useRouter } from '@tanstack/react-router';
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
import { useModule } from './module';

import type { ReactNode } from 'react';
import type { LucideComponent } from 'lucide-react';
import type { FileRouteTypes, FileRoutesById } from '@konfigyr/routeTree.gen';
import type { ModuleType } from '@konfigyr/types';
import type { Namespace } from '@konfigyr/hooks/namespace/types';

interface ModuleOption {
  id: string;
  type?: ModuleType;
  route: FileRouteTypes['id'] | string;
  icon: typeof LucideComponent;
  label: ReactNode;
  disabled?: boolean;
}

const MODULE_OPTIONS: Array<ModuleOption> = [{
  id: 'overview',
  type: 'overview',
  route: '/_authenticated/namespace/$namespace/_overview/',
  icon: HomeIcon,
  label: <FormattedMessage defaultMessage="Namespace management" description="Label for the namespace management module switcher option" />,
}, {
  id: 'services',
  type: 'services',
  route: '/_authenticated/namespace/$namespace/services/',
  icon: VaultIcon,
  label: <FormattedMessage defaultMessage="Vault" description="Label for the vault module switcher option" />,
}, {
  id: 'artifactory',
  type: 'artifactory',
  route: '/_authenticated/namespace/$namespace/artifactory/registry/',
  icon: BoxIcon,
  label: <FormattedMessage defaultMessage="Artifactory" description="Label for the artifactory module switcher option" />,
}, {
  id: 'kms',
  type: 'kms',
  route: '/_authenticated/namespace/$namespace/kms',
  icon: FolderKeyIcon,
  label: <FormattedMessage defaultMessage="KMS" description="Label for the KMS module switcher option" />,
}, {
  id: 'pki',
  route: '/_authenticated/namespace/$namespace/pki',
  icon: GlobeLockIcon,
  label: <FormattedMessage defaultMessage="PKI" description="Label for the PKI module switcher option" />,
  disabled: true,
}];

function ModuleLabel({ module, variant = 'default' }: { module: ModuleOption, variant?: 'menuitem' | 'default' }) {
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
      <span className={cn(variant === 'default' && 'font-medium text-lg grow')}>
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
  const module = useModule();

  const matching = useMemo(
    () => module ? MODULE_OPTIONS.find(it => it.type === module) : undefined,
    [module],
  );

  const onSelect = useCallback((value: string) => {
    const selected = MODULE_OPTIONS.find(it => it.id === value);
    const route = router.routesById[selected?.route as keyof FileRoutesById];
    router.navigate({ to: route.to, params: { namespace: namespace.slug } });

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
      <DropdownMenuContent className="p-2 min-w-72 font-medium text-xl">
        <DropdownMenuGroup>
          <DropdownMenuRadioGroup onValueChange={onSelect} value={matching?.id ?? ''}>
            {MODULE_OPTIONS.map(item => (
              <DropdownMenuRadioItem
                key={item.id}
                value={item.id}
                disabled={item.disabled}
                aria-disabled={item.disabled}
                className="py-1"
              >
                <ModuleLabel module={item} variant="menuitem" />
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
