'use client';

import type { Account } from 'konfigyr/services/authentication';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { LogOutIcon, Settings2Icon } from 'lucide-react';
import { logout } from 'konfigyr/app/auth/actions';
import { useAccount, AccountAvatar } from 'konfigyr/components/account';
import ServiceLabel from 'konfigyr/components/service-label';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from 'konfigyr/components/ui';

function AccountDropdown({ account }: { account: Account }) {
  const t = useTranslations('authentication');

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button type="button" className="inline-flex items-center justify-center whitespace-nowrap outline-none focus:outline-2">
          <AccountAvatar picture={account.picture} name={account.name}/>
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-68">
        <DropdownMenuLabel className="grid gap-0.5">
          <p>{account.name}</p>
          <p className="text-muted-foreground">{account.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link href="/settings">
            {t('settings')} <Settings2Icon className="ml-auto"/>
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator/>
        <DropdownMenuItem onClick={logout} className="cursor-pointer">
          {t('logout')} <LogOutIcon className="ml-auto"/>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export default function Header() {
  const t = useTranslations('authentication');
  const account = useAccount();

  return (
    <header id="header" className="border-b">
      <div className="container px-4 flex h-14 items-center gap-4">
        <Link href="/" title="Konfigyr Vault" className="text-2xl">
          <ServiceLabel/>
        </Link>

        <div className="flex flex-1 items-center justify-end">
          {account ? (
            <AccountDropdown account={account} />
          ) : (
            <Button variant="link" asChild>
              <Link href="/auth/authorize">
                {t('login')}
              </Link>
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
