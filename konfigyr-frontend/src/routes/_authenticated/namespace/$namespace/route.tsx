import {
  Layout,
  LayoutSidebar,
} from '@konfigyr/components/layout';
import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { NamespaceNavigationMenu } from '@konfigyr/components/namespace/navigation/general';
import { NamespaceKmsNavigationMenu } from '@konfigyr/components/namespace/navigation/kms';
import { NamespacePkiNavigationMenu } from '@konfigyr/components/namespace/navigation/pki';
import { NamespaceServicesNavigationMenu } from '@konfigyr/components/namespace/navigation/services';
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
      <Layout>
        <LayoutSidebar account={account} namespace={namespace}>
          <NamespaceNavigationMenu namespace={namespace} />
          <NamespaceServicesNavigationMenu namespace={namespace} />
          <NamespaceKmsNavigationMenu namespace={namespace} />
          <NamespacePkiNavigationMenu />
        </LayoutSidebar>
        <Outlet />
      </Layout>
    </NamespaceProvider>
  );
}
