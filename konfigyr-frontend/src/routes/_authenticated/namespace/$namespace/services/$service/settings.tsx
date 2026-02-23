import { ServiceUpdateForm } from '@konfigyr/components/namespace/service/update-form';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/settings',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.parentRoute.useLoaderData();

  return (
    <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
      <ServiceUpdateForm namespace={namespace} service={service} />
    </div>
  );
}
