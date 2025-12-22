import { LogOutIcon, Settings2Icon } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@konfigyr/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';

import type { Account } from '@konfigyr/hooks/types';

export function AccountDropdown({ account }: { account: Account }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button type="button" className="my-1 inline-flex items-center justify-center whitespace-nowrap outline-none focus:outline-2">
          <Avatar>
            <AvatarImage src={account.avatar} />
            <AvatarFallback title={account.fullName} />
          </Avatar>
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-68">
        <DropdownMenuLabel className="grid gap-0.5">
          <p>{account.fullName}</p>
          <p className="text-muted-foreground">{account.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link to="/account">
            Settings <Settings2Icon size="1rem" className="ml-auto"/>
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator/>
        <DropdownMenuItem className="cursor-pointer">
          Logout <LogOutIcon size="1rem" className="ml-auto"/>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
