import { FormattedMessage } from 'react-intl';
import { ActivityIcon, TrendingDownIcon } from 'lucide-react';
import { useGetAuditRecords } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { RelativeDate } from '@konfigyr/components/messages';
import { Card, CardHeader, CardIcon, CardTitle } from '@konfigyr/components/ui/card';
import { Item, ItemContent, ItemDescription, ItemGroup, ItemTitle } from '@konfigyr/components/ui/item';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { AuditRecord, AuditRecordQuery, Namespace } from '@konfigyr/hooks/types';

export const RECENT_ACTIVITY_QUERY: AuditRecordQuery = { size: 5 };

export function ActivityCardEmpty({ ...props }: ComponentProps<typeof EmptyState>) {
  return (
    <EmptyState
      icon={<TrendingDownIcon />}
      {...props}
    />
  );
}

function ActivityCardSkeleton() {
  return (
    <div data-slot="activity-item-skeleton" className="border-b-accent px-6 py-2">
      <Skeleton className="h-4 w-96 mb-2"/>
      <div className="flex items-center gap-2 mt-4">
        <Skeleton className="h-4 w-36"/>
        <Skeleton className="h-4 w-42"/>
      </div>
    </div>
  );
}

function ActivityCardItem({ record }: { record: AuditRecord }) {
  return (
    <Item
      size="sm"
      role="listitem"
      variant="list"
      aria-label={record.message}
    >
      <ItemContent>
        <ItemTitle>
          {record.message}
        </ItemTitle>
        <ItemDescription>
          <FormattedMessage
            defaultMessage="{actor} · {date}"
            description="Message used on the namespace overview activity item to display who performed the change and when. This message has an actor and a relative date property."
            values={{
              actor: (
                <span className="font-medium">
                  {record.actor.name}
                </span>
              ),
              date: (
                <RelativeDate value={record.createdAt} />
              ),
            }}
          />
        </ItemDescription>
      </ItemContent>
    </Item>
  );
}

function ActivityCardContent({ namespace }: { namespace: Namespace }) {
  const { data, error, isPending } = useGetAuditRecords(namespace, RECENT_ACTIVITY_QUERY);

  if (error) {
    return (
      <ErrorState error={error} className="border-none" />
    );
  }

  if (isPending) {
    return (
      <ActivityCardSkeleton />
    );
  }

  if (data.data.length === 0) {
    return (
      <ActivityCardEmpty title="No recent activity" />
    );
  }

  return (
    <ItemGroup size="xs" className="px-6">
      {data.data.map(record => (
        <ActivityCardItem key={record.id} record={record} />
      ))}
    </ItemGroup>
  );
}

export function ActivityCard({ namespace, className, ...props }: { namespace: Namespace } & ComponentProps<typeof Card>) {
  return (
    <Card className={cn('border', className)} {...props}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <ActivityIcon size="1.5rem"/>
          </CardIcon>
          <FormattedMessage
            defaultMessage="Recent activity"
            description="Title of the recent activity card"
          />
        </CardTitle>
      </CardHeader>
      <ActivityCardContent namespace={namespace} />
    </Card>
  );
}
