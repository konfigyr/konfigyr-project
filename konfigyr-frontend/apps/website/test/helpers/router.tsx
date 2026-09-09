import { RouterContextProvider, RouterProvider, createMemoryHistory, createRouter } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import { routeTree } from '@konfigyr/routeTree.gen';
import { NotFound } from '@konfigyr/components/routing/not-found';
import { MessagesProvider } from './messages';

import type { ReactNode } from 'react';

export function renderComponentWithRouter(ui: ReactNode) {
  const router = createRouter({
    routeTree,
    history: createMemoryHistory(),
    defaultNotFoundComponent: () => <NotFound />,
    defaultPreload: false,
    defaultPendingMinMs: 0,
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>{children}</MessagesProvider>
  );

  return {
    ...render(
      <RouterContextProvider router={router}>{ui}</RouterContextProvider>,
      { wrapper },
    ),
    router,
  };
}

export function renderWithRouter(path: string) {
  const router = createRouter({
    routeTree,
    history: createMemoryHistory({ initialEntries: [path] }),
    defaultNotFoundComponent: () => <NotFound />,
    defaultPreload: false,
    defaultPendingMinMs: 0,
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <MessagesProvider>{children}</MessagesProvider>
  );

  return {
    ...render(<RouterProvider router={router} />, { wrapper, container: document, baseElement: document }),
    router,
  };
}
