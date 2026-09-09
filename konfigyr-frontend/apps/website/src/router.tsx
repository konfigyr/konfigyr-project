import { createRouter } from '@tanstack/react-router';
import { NotFound } from '@konfigyr/components/routing/not-found';
import { routeTree } from './routeTree.gen';

export function getRouter() {
  return createRouter({
    routeTree,
    defaultNotFoundComponent: () => <NotFound />,
    defaultPreload: 'intent',
    defaultPendingMs: 100,
    defaultViewTransition: true,
  });
}

declare module '@tanstack/react-router' {
  interface Register {
    router: ReturnType<typeof getRouter>
  }
}
