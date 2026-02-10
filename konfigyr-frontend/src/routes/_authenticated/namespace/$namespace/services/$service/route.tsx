import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from '@konfigyr/components/ui/navigation-menu';
import { getNamespaceQuery, getNamespaceServiceQuery } from '@konfigyr/hooks';
import { Link, Outlet, createFileRoute } from '@tanstack/react-router';

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

function Navigation({ namespace, service }: { namespace: string, service: string }) {
  return (
    <NavigationMenu>
      <NavigationMenuList>
        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service"
              params={{ namespace, service }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Overview
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>

        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service/requests"
              params={{ namespace, service }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Change requests
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>

        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service/settings"
              params={{ namespace, service }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Settings
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>
      </NavigationMenuList>
    </NavigationMenu>
  );
}

function RouteComponent() {
  const { namespace, service } = Route.useLoaderData();

  return (
    <LayoutContent>
      <LayoutNavbar title={service.name}>
        <Navigation namespace={namespace.slug} service={service.slug} />
      </LayoutNavbar>

      <Outlet />
    </LayoutContent>
  );
}
