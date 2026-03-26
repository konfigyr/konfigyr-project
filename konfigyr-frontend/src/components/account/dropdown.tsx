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
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';

import type { Account } from '@konfigyr/hooks/types';

export function AccountDropdown({ account }: { account: Account }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <button type="button" className="my-1 inline-flex items-center justify-center whitespace-nowrap outline-none focus:outline-2">
            <Avatar>
              <AvatarImage src={account.avatar} />
              <AvatarFallback title={account.fullName} />
            </Avatar>
          </button>
        }
      />
      <DropdownMenuContent className="w-68">
        <DropdownMenuGroup>
          <DropdownMenuLabel className="grid gap-0.5">
            <p>{account.fullName}</p>
            <p className="text-muted-foreground">{account.email}</p>
          </DropdownMenuLabel>
          <DropdownMenuItem
            render={
              <Link to="/account">
                Settings <Settings2Icon size="1rem" className="ml-auto"/>
              </Link>
            }
          />
          <DropdownMenuSeparator/>
          <DropdownMenuItem className="cursor-pointer">
            Logout <LogOutIcon size="1rem" className="ml-auto"/>
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
