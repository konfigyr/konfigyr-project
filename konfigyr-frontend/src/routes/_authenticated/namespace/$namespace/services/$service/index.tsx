import { CounterStat, StatsCard } from '@konfigyr/components/reporting/stats';
import { createFileRoute } from '@tanstack/react-router';
import {
  ActivityCard,
  ActivityCardEmpty,
  ActivityCardTitle,
} from '@konfigyr/components/reporting/activity';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/services/$service/')({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
      <StatsCard>
        <CounterStat title="Environments" counter={1} />
        <CounterStat title="Active configurations" counter={124} />
        <CounterStat title="Active change requests" counter={4} />
      </StatsCard>

      <ActivityCard>
        <ActivityCardTitle>Recent activity</ActivityCardTitle>
        <ActivityCardEmpty title="No recent activity" />
      </ActivityCard>
    </div>
  );
}
