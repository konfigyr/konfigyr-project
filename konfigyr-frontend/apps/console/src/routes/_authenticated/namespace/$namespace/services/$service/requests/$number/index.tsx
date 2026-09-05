import { createFileRoute } from '@tanstack/react-router';
import { getChangeRequestQuery } from '@konfigyr/hooks';
import { ChangeRequestDetails } from '@konfigyr/components/vault/change-request/change-request-details';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/requests/$number/',
)({
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };

    // prefetch the change request...
    context.queryClient.prefetchQuery(
      getChangeRequestQuery(namespace, service, params.number),
    );

    return { namespace, service };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { number } = Route.useParams();
  const { namespace, service } = Route.useLoaderData();

  return (
    <div className="mx-4">
      <ChangeRequestDetails
        namespace={namespace}
        service={service}
        number={number}
      />
    </div>
  );
}
