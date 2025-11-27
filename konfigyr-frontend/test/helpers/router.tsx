import { RouterProvider, createMemoryHistory, createRouter } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import { routeTree } from 'konfigyr/routeTree.gen';
import { createTestQueryClient } from './query-client';

export function renderWithRouter(path: string) {
  const queryClient = createTestQueryClient();

  const router = createRouter({
    routeTree,
    context: { queryClient },
    history: createMemoryHistory({ initialEntries: [path] }),
    defaultPreload: 'intent',
    defaultPendingMinMs: 0,
  });

  return {
    ...render(<RouterProvider router={router} />),
    queryClient,
    router,
  };
}
