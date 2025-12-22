'use client';

import { cva } from 'class-variance-authority';
import { AccountDropdown } from '@konfigyr/components/account/dropdown';
import { NamespaceDropDownMenu } from '@konfigyr/components/namespace/navigation/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarProvider,
} from '@konfigyr/components/ui/sidebar';
import { cn } from '@konfigyr/components/utils';
import { SearchToggle } from './search-toggle';
import { ThemeSwitcher } from './theme-switcher';

import type { ComponentProps } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { Account, Namespace } from '@konfigyr/hooks/types';

const layoutContentVariants = cva(
  'p-4',
  {
    variants: {
      variant: {
        default: 'w-full h-screen overflow-y-auto',
        centered: 'w-full lg:w-1/2 xl:w-2/5 mx-auto',
        fullscreen: 'w-full h-screen flex flex-col items-center justify-center',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);

export function Layout({ children }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="layout"
      className="flex min-h-screen overflow-hidden"
    >
      <SidebarProvider>
        {children}
      </SidebarProvider>
    </div>
  );
}

export function LayoutNavbar({ title }: { title: string } & ComponentProps<'div'>) {
  return (
    <nav className="h-14 flex items-center justify-between px-4 py-2 mb-6 border-b">
      <h1 className="text-2xl font-semibold">{title}</h1>
      <SearchToggle />
    </nav>
  );
}

export function LayoutSidebar({ account, namespace, children }: { account: Account, namespace: Namespace } & ComponentProps<'div'>) {
  return (
    <Sidebar collapsible="none" className="h-screen">
      <SidebarHeader className="border-b">
        <NamespaceDropDownMenu namespace={namespace} />
      </SidebarHeader>
      <SidebarContent className="overflow-y-auto h-full">
        {children}
      </SidebarContent>
      <SidebarFooter className="border-t">
        <div className="flex items-center gap-2">
          <AccountDropdown account={account} />
          <ThemeSwitcher />
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}

export function LayoutContent({ variant, className, children }: VariantProps<typeof layoutContentVariants> & ComponentProps<'div'>) {
  return (
    <main
      data-slot="layout-content-container"
      className="container"
    >
      <div
        data-slot="layout-content"
        className={cn(layoutContentVariants({ variant }), className)}
      >
        {children}
      </div>
    </main>
  );
}
