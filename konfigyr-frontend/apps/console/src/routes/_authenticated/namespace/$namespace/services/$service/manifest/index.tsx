import { createFileRoute } from '@tanstack/react-router';
import { ServiceCatalog } from '@konfigyr/components/namespace/service/manifest/catalog';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/manifest/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.parentRoute.parentRoute.useLoaderData();

  return (
    <ServiceCatalog namespace={namespace} service={service} />
  );
}
