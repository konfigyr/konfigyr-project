import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { getNamespaceQuery } from '@konfigyr/hooks';
import { Outlet, createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace')({
  loader: async ({ context, params }) => {
    return await context.queryClient.ensureQueryData(getNamespaceQuery(params.namespace));
  },
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = Route.useLoaderData();

  return (
    <NamespaceProvider namespace={namespace}>
      <Outlet />
    </NamespaceProvider>
  );
}
