'use client';

import { cva } from 'class-variance-authority';
import { useIsMobile } from '@konfigyr/hooks/use-mobile';
import { AccountDropdown } from '@konfigyr/components/account/dropdown';
import { NamespaceDropDownMenu } from '@konfigyr/components/namespace/navigation/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from '@konfigyr/components/ui/sidebar';
import { cn } from '@konfigyr/components/utils';
import { ModuleSwitcher } from './module-switcher';
import { SearchToggle } from './search-toggle';
import { ThemeSwitcher } from './theme-switcher';

import type { ComponentProps, ReactNode } from 'react';
import type { VariantProps } from 'class-variance-authority';
import type { Account, Namespace } from '@konfigyr/hooks/types';

const layoutContentVariants = cva(
  'px-4',
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

export function LayoutNavbar({ title, children }: { title: ReactNode } & Omit<ComponentProps<'div'>, 'title'>) {
  const isMobile = useIsMobile();

  if (isMobile) {
    return (
      <div className="px-4 py-2 mb-6 border-b">
        <div className="flex items-center gap-2">
          <SidebarTrigger />
          <h1 className="text-2xl font-semibold grow">{title}</h1>
          <SearchToggle />
        </div>
        {children && (
          <div className="w-full overflow-auto">
            {children}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="px-4 py-2 mb-6 border-b">
      <div className="h-10 flex items-center gap-4 justify-between">
        <h1 className="text-2xl font-heading font-semibold">{title}</h1>
        {children && (
          <div className="grow">
            {children}
          </div>
        )}
        <SearchToggle />
      </div>
    </div>
  );
}

export function LayoutSidebar({ account, namespace, children }: { account: Account, namespace: Namespace } & ComponentProps<'div'>) {
  return (
    <Sidebar collapsible="offcanvas" className="h-screen">
      <SidebarHeader className="border-b">
        <ModuleSwitcher namespace={namespace} className="h-10"/>

        <div className="h-16 border-t py-2">
          <NamespaceDropDownMenu namespace={namespace} />
        </div>
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
      <SidebarRail />
    </Sidebar>
  );
}

export function LayoutContent({ variant, className, children }: VariantProps<typeof layoutContentVariants> & ComponentProps<'div'>) {
  return (
    <main
      data-slot="layout-content-container"
      className="w-full"
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
