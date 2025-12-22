import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import {
  ActivityCard,
  ActivityCardEmpty,
  ActivityCardTitle,
} from '@konfigyr/components/reporting/activity';
import { CounterStat, StatsCard } from '@konfigyr/components/reporting/stats';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/')({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <LayoutContent>
      <LayoutNavbar title="Overview"/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <StatsCard>
          <CounterStat title="Active services" counter={1} />
          <CounterStat title="Active configurations" counter={124} />
          <CounterStat title="Active change requests" counter={4} />
          <CounterStat title="Active members" counter={3} />
        </StatsCard>

        <ActivityCard>
          <ActivityCardTitle>Recent activity</ActivityCardTitle>
          <ActivityCardEmpty title="No recent activity" />
        </ActivityCard>
      </div>
    </LayoutContent>
  );
}
