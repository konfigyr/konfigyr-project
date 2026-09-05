import { z } from 'zod';
import { createFileRoute } from '@tanstack/react-router';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { ChangeRequestList } from '@konfigyr/components/vault/change-request/change-request-list';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeRequestQuery } from '@konfigyr/hooks/vault/types';

const searchQuerySchema = z.object({
  term: z.string().optional(),
  profile: z.string().optional(),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/requests/',
)({
  validateSearch: searchQuerySchema,
  loader: async ({ parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };
    return { namespace, service };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service } = Route.useLoaderData();
  const navigate = Route.useNavigate();
  const query: ChangeRequestQuery = Route.useSearch({
    select: (search: ChangeRequestQuery) => ({
      ...search,
      state: search.state ?? ChangeRequestState.OPEN,
      page: search.page ?? 1,
      size: search.size ?? 20,
    }),
  });

  return (
    <div className="mx-4 space-y-6">
      <ChangeRequestList
        namespace={namespace}
        service={service}
        query={query}
        onQueryChange={search => navigate({ search })}
      />
    </div>
  );
}
