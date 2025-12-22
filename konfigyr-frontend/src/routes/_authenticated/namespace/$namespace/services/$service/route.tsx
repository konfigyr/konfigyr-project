import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { getNamespaceQuery, getNamespaceServiceQuery } from '@konfigyr/hooks';
import { Outlet, createFileRoute } from '@tanstack/react-router';

const generatePageTitle = (namespace?: string, service?: string) => {
  if (namespace && service) {
    return `${service} | ${namespace} | Konfigyr`;
  }
  return 'Konfigyr';
};

export const Route = createFileRoute('/_authenticated/namespace/$namespace/services/$service')({
  loader: async ({ context, params }) => {
    const namespace = await context.queryClient.ensureQueryData(getNamespaceQuery(params.namespace));
    const service = await context.queryClient.ensureQueryData(getNamespaceServiceQuery(params.namespace, params.service));

    return { namespace, service };
  },
  component: RouteComponent,
  head: ({ loaderData }) => ({
    meta: [{
      title: generatePageTitle(loaderData?.namespace.name, loaderData?.service.name),
    }],
  }),
});

function RouteComponent() {
  const { service } = Route.useLoaderData();

  return (
    <LayoutContent>
      <LayoutNavbar title={service.name} />
      <Outlet />
    </LayoutContent>
  );
}
