import { FileStackIcon, HistoryIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { RelativeDate } from '@konfigyr/components/messages';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useGetChangeHistory } from '@konfigyr/hooks';
import * as React from 'react';
import { ErrorState } from '@konfigyr/components/error';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { SimplePagination } from '@konfigyr/components/vault/change-history/simple-pagination';
import { Card, CardContent, CardHeader, CardIcon, CardTitle } from '@konfigyr/components/ui/card';
import { Item,
  ItemContent,
  ItemDescription, ItemGroup,
  ItemTitle } from '@konfigyr/components/ui/item';
import type { ChangeHistory, Profile } from '@konfigyr/hooks/vault/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

export type ChangeHistoryTimelineProps = {
  namespace: Namespace,
  service: Service,
  profile: Profile,
};

export type DailyChangesetHistoryProps = {
  date: string,
  items: Array<ChangeHistory>
};

export type DailyChangesetHistoryItemProps = {
  item: ChangeHistory
};

export function ChangeHistoryTimeline({ namespace, service, profile }: ChangeHistoryTimelineProps) {
  const [page, setPage] = useState(0);

  const { data: history, error, isPending, isError, refetch } = useGetChangeHistory(namespace, service, profile, {
    page,
  });

  useEffect(() => { refetch(); }, [page]);

  const onPageChange = useCallback((p: number) => setPage(p), [page]);

  const groupedByDate = useMemo(() => {
    return history?.data.reduce<Record<string, Array<ChangeHistory>>>((acc, record) => {
      const date = record.appliedAt.split('T')[0];
      if (!acc.hasOwnProperty(date)) {
        acc[date] = [];
      }
      acc[date].push(record);
      return acc;
    }, {}) ?? {};
  }, [history]);

  if (isPending) {
    return (
      <ChangeHistorySkeleton />
    );
  }

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  if (history.data.length === 0) {
    return (
      <EmptyState
        icon={<FileStackIcon />}
        title={
          <FormattedMessage
            defaultMessage="No history found for this property."
            description="Empty state title used when no history is found for a property."
          />
        }
      />
    );
  }

  return (
    <>
      {Object.keys(groupedByDate).map((date) => (
        <ChangeHistory
          key={date}
          date={date}
          items={groupedByDate[date]}
        />
      ))}

      <div>
        <SimplePagination
          page={history.metadata.number}
          pages={history.metadata.pages}
          onClick={onPageChange} />
      </div>

    </>
  );
}

function ChangeHistorySkeleton() {
  return (
    <div data-slot="changeset-history-skeleton" className="w-full">
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2 min-w-0 flex-1">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-64" />
        </div>
        <div className="flex items-center gap-4 shrink-0">
          <Skeleton className="h-3 w-72" />
          <Skeleton className="h-3 w-20" />
        </div>
      </div>
    </div>
  );
}

function ChangeHistory({ date, items }: DailyChangesetHistoryProps) {
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <HistoryIcon size="1.25rem"/>
          </CardIcon>
          <FormattedMessage
            defaultMessage="Commits on {date}"
            description="Expiration date for the application"
            values={{
              date: new Intl.DateTimeFormat(navigator.language, {
                month: 'short',
                day: '2-digit',
                year: 'numeric',
              }).format(new Date(date)),
            }}
          />
        </CardTitle>
      </CardHeader>
      <CardContent>
        <ItemGroup className="-mx-4">
          {items.map(artifact => (
            <ChangeHistoryItem key={artifact.id} item={artifact}/>
          ))}
        </ItemGroup>
      </CardContent>
    </Card>
  );
}

function ChangeHistoryItem({ item }: DailyChangesetHistoryItemProps) {
  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle>
          {item.description}
        </ItemTitle>
        <ItemDescription>
          {item.appliedBy}
        </ItemDescription>
      </ItemContent>
      <div>{item.id}</div>
      <div>
        <RelativeDate value={item.appliedAt} />
      </div>
    </Item>
  );
}


