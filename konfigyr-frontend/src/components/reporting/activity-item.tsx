import { FormattedMessage } from 'react-intl';
import { useGetAuditRecords } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { RelativeDate } from '@konfigyr/components/messages/relative-date';
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { ActivityCardEmpty } from './activity';

import type { AuditRecord, AuditRecordQuery, Namespace } from '@konfigyr/hooks/types';

export const RECENT_ACTIVITY_QUERY: AuditRecordQuery = { size: 5 };

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
                <span className="font-medium text-foreground">
                  {record.actor.name}
                </span>
              ),
              date: (
                <RelativeDate
                  value={record.createdAt}
                  className="font-medium text-foreground"
                />
              ),
            }}
          />
        </ItemDescription>
      </ItemContent>
    </Item>
  );
}

export function ActivityCardList({ namespace }: { namespace: Namespace }) {
  const { data, error, isPending } = useGetAuditRecords(namespace, RECENT_ACTIVITY_QUERY);

  if (error) {
    return (
      <ErrorState error={error} className="mx-6" />
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
