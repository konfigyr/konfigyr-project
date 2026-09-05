import {
  Layout,
  LayoutSidebar,
  ModuleProvider,
} from '@konfigyr/layout';
import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { ModuleNavigation } from '@konfigyr/components/namespace/navigation/module';
import { getNamespaceQuery, useAccount } from '@konfigyr/hooks';
import { Outlet, createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace')({
  loader: async ({ context, params }) => {
    return await context.queryClient.ensureQueryData(getNamespaceQuery(params.namespace));
  },
  component: RouteComponent,
  head: ({ loaderData }) => ({
    meta: [{
      title: loaderData?.name ? `${loaderData.name} | Konfigyr` : 'Konfigyr',
    }],
  }),
});

function RouteComponent() {
  const account = useAccount();
  const namespace = Route.useLoaderData();

  return (
    <NamespaceProvider namespace={namespace}>
      <ModuleProvider>
        <Layout>
          <LayoutSidebar account={account} namespace={namespace}>
            <ModuleNavigation namespace={namespace} />
          </LayoutSidebar>
          <Outlet />
        </Layout>
      </ModuleProvider>
    </NamespaceProvider>
  );
}
