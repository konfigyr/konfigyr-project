import { useCallback } from 'react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import { useGetAuditRecords } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { CursorPagination } from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';

import { AuditEntityTypeIcon } from './audit-entity-type';
import { AuditRecordFilters } from './audit-record-filters';

import type { AuditRecord, AuditRecordQuery, CursorResponse, Namespace } from '@konfigyr/hooks/types';

function AuditRecordListItem({ record }: { record: AuditRecord }) {
  return (
    <Item
      size="sm"
      role="listitem"
      variant="list"
      aria-label={record.message}
    >
      <ItemMedia variant="icon">
        <AuditEntityTypeIcon value={record.entityType} className="size-6" />
      </ItemMedia>
      <ItemContent>
        <ItemTitle>
          {record.message}
        </ItemTitle>
        <ItemDescription>
          <FormattedMessage
            defaultMessage="Performed by {actor} on {date}"
            description="Message used on the audit record item to display who and when the change was performed. This message has an actor and a date property."
            values={{
              actor: (
                <span className="font-medium text-foreground">
                  {record.actor.name}
                </span>
              ),
              date: (
                <time className="font-medium text-foreground" dateTime={record.createdAt}>
                  <FormattedDate
                    value={record.createdAt}
                    year="numeric"
                    month="short"
                    day="numeric"
                    hour="numeric"
                    minute="2-digit"
                    hourCycle="h24"
                  />
                </time>
              ),
            }}
          />
        </ItemDescription>
      </ItemContent>
    </Item>
  );
}

function AuditRecordSkeleton() {
  return (
    <div data-slot="audit-records-list-skeleton" className="border border-border/60 rounded-lg px-3 py-4 space-y-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2 min-w-0">
          <Skeleton className="size-6 rounded-lg" />
        </div>
        <div className="flex flex-col flex-1 gap-2">
          <Skeleton className="h-4 w-72" />
          <Skeleton className="h-4 w-48" />
        </div>
      </div>
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2 min-w-0">
          <Skeleton className="size-6 rounded-lg" />
        </div>
        <div className="flex flex-col flex-1 gap-2">
          <Skeleton className="h-4 w-96" />
          <Skeleton className="h-4 w-lg" />
        </div>
      </div>
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-2 min-w-0">
          <Skeleton className="size-6 rounded-lg" />
        </div>
        <div className="flex flex-col flex-1 gap-2">
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-4 w-64" />
        </div>
      </div>
    </div>
  );
}

function AuditRecordListContent({ data, error, isPending = false }: {
  data?: CursorResponse<AuditRecord>;
  error?: Error | null;
  isPending?: boolean;
}) {
  if (isPending) {
    return (
      <AuditRecordSkeleton />
    );
  }

  if (error) {
    return (
      <ErrorState error={error} />
    );
  }

  if (data?.data.length === 0) {
    return (
      <EmptyState
        title={
          <FormattedMessage
            defaultMessage="No audit records found."
            description="Empty state title used when no audit records are found."
          />
        }
        description={
          <FormattedMessage
            defaultMessage="Could not find any audit records matching your search criteria."
            description="Empty state description used when no audit records are found. Should tell the user to update the filter criteria."
          />
        }
        size="lg"
      />
    );
  }

  return (
    <ItemGroup size="xs" className="border rounded-lg px-3 py-1">
      {data?.data.map(record => (
        <AuditRecordListItem
          key={record.id}
          record={record}
        />
      ))}
    </ItemGroup>
  );
}

function AuditRecordPagination({ size = 20, data, onChange }: {
  size?: number;
  data?: CursorResponse<AuditRecord>;
  onChange: (token: string, size: number) => void;
}) {
  return (
    <CursorPagination
      size={size}
      next={data?.metadata.next}
      previous={data?.metadata.previous}
      onChange={onChange}
    />
  );
}

export function AuditRecordList({ namespace, query, onQueryChange }: {
  namespace: Namespace;
  query: AuditRecordQuery;
  onQueryChange: (query: AuditRecordQuery) => void;
}) {
  const { data, error, isPending } = useGetAuditRecords(namespace, query);

  const onPaginationChange = useCallback((token: string, size: number) => {
    onQueryChange({ ...query, token, size });
  }, [query]);

  return (
    <>
      <AuditRecordFilters
        query={query}
        onQueryChange={onQueryChange}
      />

      <AuditRecordListContent
        data={data}
        error={error}
        isPending={isPending}
      />

      <AuditRecordPagination
        size={query.size}
        data={data}
        onChange={onPaginationChange}
      />
    </>
  );
}
