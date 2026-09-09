/// <reference types="vite/client" />
import {
  HeadContent,
  Outlet,
  Scripts,
  createRootRoute,
} from '@tanstack/react-router';
import { IntlProvider } from 'react-intl';
import { Header } from '@konfigyr/components/layout/header';
import { Footer } from '@konfigyr/components/layout/footer';
import styles from '@konfigyr/styles.css?url';

import type { ReactNode } from 'react';

export const Route = createRootRoute({
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
        title: 'Konfigyr: Configuration management for Spring Boot',
      },
      {
        name: 'description',
        content: 'Konfigyr reads the property metadata your Spring Boot build already generates, so every config value is validated, tracked across versions, and reviewed before it reaches production.',
      },
    ],
    links: [
      {
        rel: 'stylesheet',
        href: styles,
      },
      {
        rel: 'icon',
        href: '/favicon.ico',
      },
    ],
  }),
  component: RootComponent,
  ssr: true,
});

function RootComponent() {
  return (
    <IntlProvider locale="en" messages={{}}>
      <RootDocument>
        <div className="flex min-h-svh flex-col">
          <Header />
          <main className="flex-1">
            <Outlet />
          </main>
          <Footer />
        </div>
      </RootDocument>
    </IntlProvider>
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
