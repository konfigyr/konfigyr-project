import { RouterContextProvider, RouterProvider, createMemoryHistory, createRouter } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import { routeTree } from '@konfigyr/routeTree.gen';
import { ErrorBoundary } from '@konfigyr/components/routing/error';
import { NotFound } from '@konfigyr/components/routing/not-found';
import { QueryClientProvider } from '@tanstack/react-query';
import { createTestQueryClient } from './query-client';
import type { ReactNode } from 'react';

export function renderComponentWithRouter(ui: ReactNode) {
  const queryClient = createTestQueryClient();

  const router = createRouter({
    routeTree,
    context: { queryClient },
    history: createMemoryHistory(),
    defaultErrorComponent: ErrorBoundary,
    defaultNotFoundComponent: () => <NotFound />,
    defaultPreload: 'intent',
    defaultPendingMinMs: 0,
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return {
    ...render(
      (<RouterContextProvider router={router}>{ui}</RouterContextProvider>),
      { wrapper },
    ),
    queryClient,
    router,
  };
}

export function renderWithRouter(path: string) {
  const queryClient = createTestQueryClient();

  const router = createRouter({
    routeTree,
    context: { queryClient },
    history: createMemoryHistory({ initialEntries: [path] }),
    defaultErrorComponent: ErrorBoundary,
    defaultNotFoundComponent: () => <NotFound />,
    defaultPreload: 'intent',
    defaultPendingMinMs: 0,
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return {
    ...render(<RouterProvider router={router} />, { wrapper }),
    queryClient,
    router,
  };
}
