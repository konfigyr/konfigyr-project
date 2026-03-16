import { LayoutContent } from '@konfigyr/components/layout';
import { Outlet, createFileRoute } from '@tanstack/react-router';
import { getProfileQuery } from '@konfigyr/hooks';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

const generatePageTitle = (namespace?: string, service?: string, profile?: string) => {
  if (profile && namespace && service) {
    return `${profile} | ${service} | ${namespace} | Konfigyr`;
  }
  return 'Konfigyr';
};

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/profiles/$profile',
)({
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };

    const profile = await context.queryClient.ensureQueryData(getProfileQuery(namespace, service, params.profile));

    return { namespace, service, profile };
  },
  head: ({ loaderData }) => ({
    meta: [{
      title: generatePageTitle(
        loaderData?.namespace.name,
        loaderData?.service.name,
        loaderData?.profile.name,
      ),
    }],
  }),
  component: NamespaceApplicationsLayoutComponent,
});

function NamespaceApplicationsLayoutComponent() {
  return (
    <LayoutContent>
      <Outlet />
    </LayoutContent>
  );
}
