import { useState } from 'react';
import { ChevronDownIcon, PlusIcon } from 'lucide-react';
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
import {
  SidebarMenuButton,
} from '@konfigyr/components/ui/sidebar';
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
        <p className="font-mono text-sm">
          {namespace.slug}
        </p>
      </div>
    </div>
  );
}

export function NamespaceDropDownMenu({ namespace }: { namespace: Namespace }) {
  const [open, onOpenChange] = useState(false);
  const { data: namespaces } = useGetNamespaces();

  return (
    <DropdownMenu open={open} onOpenChange={onOpenChange}>
      <DropdownMenuTrigger asChild>
        <SidebarMenuButton className="w-full h-auto flex items-center justify-between gap-2">
          <NamespaceItem namespace={namespace} size="size-10" />
          <ChevronDownIcon
            className={cn('relative top-[1px] ml-1 size-3 transition duration-300', open && 'rotate-180')}
            aria-hidden="true"
          />
        </SidebarMenuButton>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuGroup>
          {namespaces?.filter(item => item.id !== namespace.id).map((item) => (
            <DropdownMenuItem key={item.id} asChild>
              <Link to="/namespace/$namespace" params={{ namespace: item.slug }}>
                <NamespaceItem namespace={item} size="size-8" />
              </Link>
            </DropdownMenuItem>
          ))}
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/namespace/provision">
            <PlusIcon size="1rem" aria-hidden="true"/>
            Create new namespace
          </Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
