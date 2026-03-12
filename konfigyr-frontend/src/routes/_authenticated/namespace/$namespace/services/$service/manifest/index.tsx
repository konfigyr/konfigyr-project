import { createFileRoute } from '@tanstack/react-router';
import { ServiceManifest } from '@konfigyr/components/namespace/service/manifest';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/manifest/',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.parentRoute.useLoaderData();

  return (
    <div className="lg:w-2/3 xl:w-3/5 px-4 mx-auto">
      <ServiceManifest namespace={namespace} service={service} />
    </div>
  );
}
