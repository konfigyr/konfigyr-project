import { AccountProvider } from '@konfigyr/components/account/context';
import { Outlet, createFileRoute } from '@tanstack/react-router';

/**
 * Pathless route that wraps the entire application in the account context provider. The account context
 * provider provides the authenticated user account to all child routes. In our particular case, most of the
 * application routes are protected by the account context provider.
 *
 * Please check out the documentation of the `Tanstack React Router` for more information about pathless
 * layout routes and how they work.
 *
 * @see https://tanstack.com/router/latest/docs/framework/react/routing/routing-concepts#pathless-layout-routes
 */
export const Route = createFileRoute('/_authenticated')({
  component: RouteComponent,
  ssr: true,
});

function RouteComponent() {
  return (
    <AccountProvider>
      <Outlet />
    </AccountProvider>
  );
}
