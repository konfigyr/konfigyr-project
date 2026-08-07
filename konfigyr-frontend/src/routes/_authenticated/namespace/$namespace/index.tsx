import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { ActivityCard, RECENT_ACTIVITY_QUERY } from '@konfigyr/components/reporting/activity';
import { DashboardStats } from '@konfigyr/components/reporting/dashboard-stats';
import { getAuditRecordsQuery, getDashboardQuery, useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';

import type { Namespace } from '@konfigyr/hooks/types';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/')({
  loader: async ({ context, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const namespace = match.loaderData as Namespace;

    await Promise.all([
      context.queryClient.prefetchQuery(getDashboardQuery(namespace)),
      context.queryClient.prefetchQuery(getAuditRecordsQuery(namespace, RECENT_ACTIVITY_QUERY)),
    ]);
  },
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();

  return (
    <LayoutContent>
      <LayoutNavbar title="Overview"/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <DashboardStats namespace={namespace} />
        <ActivityCard namespace={namespace} />
      </div>
    </LayoutContent>
  );
}
