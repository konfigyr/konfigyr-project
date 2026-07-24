import { FormattedMessage } from 'react-intl';
import { ChevronRightIcon } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import { useGetDashboard } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { CounterStat, StatsCard } from './stats';

import type { Namespace } from '@konfigyr/hooks/types';

function SkeletonStat() {
  return (
    <div data-slot="skeleton-stat" className="col-span-1 flex flex-col gap-2 border-r last:border-none">
      <Skeleton className="h-4 w-28" />
      <Skeleton className="h-6 w-6" />
    </div>
  );
}

export function DashboardStats({ namespace }: { namespace: Namespace }) {
  const { data, error, isPending } = useGetDashboard(namespace);

  if (error) {
    return (
      <StatsCard>
        <ErrorState error={error} className="col-span-full" />
      </StatsCard>
    );
  }

  if (isPending) {
    return (
      <StatsCard>
        <SkeletonStat />
        <SkeletonStat />
        <SkeletonStat />
        <SkeletonStat />
        <SkeletonStat />
      </StatsCard>
    );
  }

  return (
    <StatsCard>
      <CounterStat
        title={(
          <FormattedMessage
            defaultMessage="Active services"
            description="Title of the active services stat tile on the namespace dashboard"
          />
        )}
        counter={data.activeServices}
        footer={(
          <FormattedMessage
            defaultMessage="Deployed here"
            description="Footer label of the active services stat tile on the namespace dashboard"
          />
        )}
      />
      <CounterStat
        title={(
          <FormattedMessage
            defaultMessage="Active configurations"
            description="Title of the active configurations stat tile on the namespace dashboard"
          />
        )}
        counter={data.activeConfigurations}
        footer={(
          <FormattedMessage
            defaultMessage="{count, plural, =0 {No services created} one {Across # service} other {Across # services}}"
            description="Footer label of the active configurations stat tile on the namespace dashboard"
            values={{ count: data.activeServices }}
          />
        )}
      />
      <CounterStat
        title={(
          <FormattedMessage
            defaultMessage="Open change requests"
            description="Title of the open change requests stat tile on the namespace dashboard"
          />
        )}
        counter={data.openChangeRequests}
        footer={(
          <FormattedMessage
            defaultMessage="{count, plural, =0 {All caught up} other {Awaiting review}}"
            description="Footer label of the open change requests stat tile on the namespace dashboard"
            values={{ count: data.openChangeRequests }}
          />
        )}
      />
      <CounterStat
        title={(
          <FormattedMessage
            defaultMessage="Artifacts owned"
            description="Title of the artifacts owned stat tile on the namespace dashboard"
          />
        )}
        counter={data.artifactsOwned}
        cta={(
          <Link
            to="/namespace/$namespace/artifactory/registry"
            params={{ namespace: namespace.slug }}
            className="flex items-center justify-between gap-1"
          >
            <FormattedMessage
              defaultMessage="Browse artifacts"
              description="Link label in the artifacts owned stat tile on the namespace dashboard"
            />
            <ChevronRightIcon/>
          </Link>
        )}
      />
      <CounterStat
        title={(
          <FormattedMessage
            defaultMessage="Active members"
            description="Title of the active members stat tile on the namespace dashboard"
          />
        )}
        counter={data.members.limit === null ? data.members.count : `${data.members.count} / ${data.members.limit}`}
        cta={(
          <Link
            to="/namespace/$namespace/members"
            params={{ namespace: namespace.slug }}
            className="flex items-center justify-between gap-1"
          >
            <FormattedMessage
              defaultMessage="Manage members"
              description="Link label in the active members stat tile on the namespace dashboard"
            />
            <ChevronRightIcon />
          </Link>
        )}
      />
    </StatsCard>
  );
}
