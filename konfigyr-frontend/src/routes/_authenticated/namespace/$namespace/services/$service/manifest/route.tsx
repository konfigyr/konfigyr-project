import { FormattedMessage } from 'react-intl';
import { Outlet, createFileRoute } from '@tanstack/react-router';
import { ManifestMenu } from '@konfigyr/components/namespace/service/manifest/menu';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/manifest',
)({
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.parentRoute.useLoaderData();

  return (
    <div className="mx-4 space-y-6">
      <div>
        <p className="font-medium text-xl/relaxed">
          <FormattedMessage
            defaultMessage="Service manifest"
            description="Title of the service configuration manifest page"
          />
        </p>
        <p className="text-muted-foreground text-sm/relaxed">
          <FormattedMessage
            defaultMessage="Explore the service configuration property metadata and artifact information."
            description="Subtitle of service configuration manifest page"
          />
        </p>
      </div>

      <ManifestMenu namespace={namespace} service={service} />

      <Outlet />
    </div>
  );
}
