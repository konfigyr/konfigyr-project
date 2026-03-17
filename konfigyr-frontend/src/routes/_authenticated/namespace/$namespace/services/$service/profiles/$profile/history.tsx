import {
  getProfileQuery,
} from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { ChangeHistoryTimeline } from '@konfigyr/components/vault/change-history/change-history';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';


export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/profiles/$profile/history',
)({
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };
    const profile = await context.queryClient.ensureQueryData(getProfileQuery(namespace, service, params.profile));

    return { namespace, service, profile };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service, profile } = Route.useLoaderData();
  return (
    <div className="w-full space-y-6 px-4 mx-auto">
      <ChangeHistoryTimeline
        namespace={namespace}
        service={service}
        profile={profile}
      />
    </div>
  );
}
