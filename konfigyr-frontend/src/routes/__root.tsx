/// <reference types="vite/client" />
import { IntlProvider } from 'react-intl';
import {
  HeadContent,
  Outlet,
  Scripts,
  createRootRouteWithContext,
} from '@tanstack/react-router';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { Toaster } from '@konfigyr/components/ui/sonner';
import styles from '@konfigyr/styles.css?url';
import defaultMessages from '@konfigyr/translations/en.json';

import type { ReactNode } from 'react';
import type { QueryClient } from '@tanstack/react-query';

export interface RouteContext {
  queryClient: QueryClient;
}

export const Route = createRootRouteWithContext<RouteContext>()({
  head: () => ({
    meta: [
      {
        charSet: 'utf-8',
      },
      {
        name: 'viewport',
        content: 'width=device-width, initial-scale=1',
      },
      {
        title: 'Konfigyr',
      },
    ],
    links: [
      {
        rel: 'stylesheet',
        href: styles,
      },
    ],
  }),
  component: RootComponent,
  ssr: true,
});

function RootComponent() {
  return (
    <RootDocument>
      <IntlProvider locale="en" messages={defaultMessages}>
        <Outlet />
        <Toaster />
      </IntlProvider>
      <ReactQueryDevtools />
      <TanStackRouterDevtools />
    </RootDocument>
  );
}

function RootDocument({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html>
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}
