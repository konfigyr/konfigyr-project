import { createFileRoute } from '@tanstack/react-router';
import { ServiceArtifacts } from '@konfigyr/components/namespace/service/manifest/artifacts';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/manifest/artifacts',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.parentRoute.parentRoute.useLoaderData();

  return (
    <ServiceArtifacts namespace={namespace} service={service} />
  );
}
