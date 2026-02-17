import { createFileRoute, redirect } from '@tanstack/react-router';
import { getProfilesQuery } from '@konfigyr/hooks';

import type { Namespace, Service } from '@konfigyr/hooks/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/',
)({
  loader: async ({ context, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };

    const profiles = await context.queryClient.ensureQueryData(getProfilesQuery(namespace, service));

    if (profiles.length === 0) {
      throw redirect({
        to: '/namespace/$namespace/services/$service/create-profile',
        params: { namespace: namespace.slug, service: service.slug },
      });
    }

    throw redirect({
      to: '/namespace/$namespace/services/$service/profiles/$profile',
      params: { namespace: namespace.slug, service: service.slug, profile: profiles[0].slug },
    });
  },
});
