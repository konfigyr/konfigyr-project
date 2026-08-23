import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/layout';
import { ActivityCard, RECENT_ACTIVITY_QUERY } from '@konfigyr/components/reporting/activity';
import { DashboardStats } from '@konfigyr/components/reporting/dashboard-stats';
import { getAuditRecordsQuery, getDashboardQuery, getNamespaceQuery, useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/_overview/')({
  loader: async ({ context, params }) => {
    const namespace = await context.queryClient.ensureQueryData(getNamespaceQuery(params.namespace));

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
