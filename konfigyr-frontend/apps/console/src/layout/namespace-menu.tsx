import { useMemo, useState } from 'react';
import { ChevronDownIcon, PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useGetNamespaces } from '@konfigyr/hooks';
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@konfigyr/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { SidebarMenuButton } from '@konfigyr/components/ui/sidebar';
import { Link } from '@tanstack/react-router';
import { cn } from '@konfigyr/components/utils';

import type { Namespace } from '@konfigyr/hooks/namespace/types';

function NamespaceItem({ namespace, size }: { namespace: Namespace, size?: string }) {
  return (
    <div className="flex items-center gap-2">
      <Avatar className={cn(size)}>
        <AvatarImage src={namespace.avatar} alt={namespace.name} />
        <AvatarFallback>{namespace.name}</AvatarFallback>
      </Avatar>

      <div className="text-left">
        <p className="font-medium text-ellipsis whitespace-nowrap overflow-hidden">
          {namespace.name}
        </p>
        <p className="font-mono text-xs text-muted-foreground">
          {namespace.slug}
        </p>
      </div>
    </div>
  );
}

function NamespaceMenuGroup({ namespaces }: { namespaces?: Array<Namespace> }) {
  if (!namespaces || namespaces.length === 0) {
    return null;
  }

  return (
    <>
      <DropdownMenuGroup>
        {namespaces.map((item) => (
          <DropdownMenuItem
            key={item.id}
            render={
              <Link to="/namespace/$namespace" params={{ namespace: item.slug }}>
                <NamespaceItem namespace={item} size="size-8" />
              </Link>
            }
          />
        ))}
      </DropdownMenuGroup>
      <DropdownMenuSeparator />
    </>
  );
}

export function NamespaceSidebarMenu({ namespace }: { namespace: Namespace }) {
  const [open, onOpenChange] = useState(false);
  const { data } = useGetNamespaces();

  const namespaces = useMemo(
    () => data?.filter(item => item.id !== namespace.id),
    [data, namespace.id],
  );

  return (
    <DropdownMenu open={open} onOpenChange={onOpenChange}>
      <DropdownMenuTrigger
        render={
          <SidebarMenuButton className="w-full h-auto flex items-center justify-between gap-2 px-2 py-1">
            <NamespaceItem namespace={namespace} size="size-10" />
            <ChevronDownIcon
              className={cn('relative top-[1px] ml-1 size-3 transition duration-300', open && 'rotate-180')}
              aria-hidden="true"
            />
          </SidebarMenuButton>
        }
      />
      <DropdownMenuContent>
        <NamespaceMenuGroup namespaces={namespaces} />
        <DropdownMenuItem
          render={
            <Link to="/namespace/provision">
              <PlusIcon size="1rem" aria-hidden="true"/>
              <FormattedMessage
                defaultMessage="Create new namespace"
                description="Label used in the Namespace switcher to create a new namespace."
              />
            </Link>
          }
        />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
