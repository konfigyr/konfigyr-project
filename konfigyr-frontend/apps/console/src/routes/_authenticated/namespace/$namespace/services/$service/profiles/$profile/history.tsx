import { z } from 'zod';
import { useCallback, useState } from 'react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { getProfileQuery, useGetChangeHistory } from '@konfigyr/hooks';
import { ChangeHistorySidebar } from '@konfigyr/components/vault/change-history/change-history-sidebar';
import { ChangeHistoryTimeline } from '@konfigyr/components/vault/change-history/change-history-timeline';
import { CursorPagination } from '@konfigyr/components/ui/pagination';

import type { ChangeHistory, ChangeHistoryQuery, Namespace, Service } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  size: z.number().optional().catch(undefined),
  token: z.string().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/profiles/$profile/history',
)({
  validateSearch: searchQuerySchema,
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };
    const profile = await context.queryClient.ensureQueryData(getProfileQuery(namespace, service, params.profile));

    return { namespace, service, profile };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [selectedHistory, setSelectedHistory] = useState<ChangeHistory | null>(null);

  const navigate = useNavigate({ from: Route.fullPath });
  const query: ChangeHistoryQuery = Route.useSearch();
  const { namespace, service, profile } = Route.useLoaderData();

  const { data, error, isPending } = useGetChangeHistory(namespace, service, profile, {
    size: query.size ?? 20,
    token: query.token,
  });

  const onSelectChangeHistory = useCallback((history: ChangeHistory) => {
    setSelectedHistory(history);
    setSidebarOpen(true);
  }, []);

  return (
    <div className="w-full space-y-6 px-4 mx-auto">
      <ChangeHistoryTimeline
        history={data?.data}
        isPending={isPending}
        error={error}
        onSelect={onSelectChangeHistory}
      />

      <ChangeHistorySidebar
        open={sidebarOpen}
        history={selectedHistory}
        namespace={namespace}
        service={service}
        profile={profile}
        onOpenChange={setSidebarOpen}
      />

      {data?.metadata && (
        <CursorPagination
          size={query.size ?? 20}
          next={data.metadata.next}
          previous={data.metadata.previous}
          onChange={(token, size) => navigate({
            search: current => ({ ...current, token, size }),
            viewTransition: false,
          })}
        />
      )}
    </div>
  );
}
