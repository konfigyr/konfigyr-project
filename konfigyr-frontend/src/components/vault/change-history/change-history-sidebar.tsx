'use client';

import { useMemo } from 'react';
import { FormattedDate, FormattedMessage } from 'react-intl';
import {
  GalleryVerticalEndIcon,
  MinusIcon,
  PlusIcon,
} from 'lucide-react';
import { useGetChangeHistoryDetails } from '@konfigyr/hooks';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { ErrorState } from '@konfigyr/components/error';
import { MissingPropertyDescriptionLabel } from '@konfigyr/components/artifactory/messages';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { RelativeDate } from '@konfigyr/components/messages/relative-date';
import { ChangesCountLabel } from '@konfigyr/components/vault/messages';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@konfigyr/components/ui/sheet';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { ScrollArea } from '@konfigyr/components/ui/scroll-area';
import { cn } from '@konfigyr/components/utils';
import { PropertyTransitionTypeLabel } from './property-transition-type';

import type { ReactNode } from 'react';
import type {
  ChangeHistory,
  ChangeHistoryRecord,
  Namespace,
  Profile,
  Service,
} from '@konfigyr/hooks/types';

export interface ChangeHistorySidebar {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  history: ChangeHistory | null;
  namespace: Namespace;
  service: Service;
  profile: Profile;
}

function ChangeHistoryTimestamp({ history }: { history: ChangeHistory }) {
  return (
    <>
      <FormattedDate
        value={history.appliedAt}
        year="numeric"
        month="long"
        day="numeric"
        hour="2-digit"
        minute="2-digit"
        second="2-digit"
      />
      <small className="text-xs text-muted-foreground ml-1">
        (<RelativeDate value={history.appliedAt} />)
      </small>
    </>
  );
}

function ChangeHistorySkeleton() {
  return (
    <div data-slot="changeset-history-skeleton" className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <Skeleton className="size-5 rounded-full" />
        <Skeleton className="h-4 w-16" />
      </div>
      <Skeleton className="h-3 w-48" />
      <Skeleton className="h-8" />
      <div className="flex items-center gap-2">
        <Skeleton className="size-5 rounded-full" />
        <Skeleton className="h-4 w-20" />
      </div>
      <Skeleton className="h-3 w-96" />
      <Skeleton className="h-16" />
    </div>
  );
}

function ChangeHistoryPropertyValue({ variant, value, className }: { variant: 'addition' | 'removal', value: ReactNode, className?: string }) {
  return (
    <div className={cn(
      'flex items-center gap-2 px-3 py-1 leading-loose',
      variant === 'removal' && 'bg-destructive/5 text-destructive/80',
      variant === 'addition' && 'bg-emerald-500/5 text-emerald-700',
      className,
    )}>
      <span className="select-none shrink-0">
        {variant === 'removal' ? <MinusIcon className="size-2" /> : <PlusIcon className="size-2" />}
      </span>
      <span className="break-all font-medium">{value}</span>
    </div>
  );
}

function ChangeHistoryStat({ label, value }: { label: ReactNode, value: ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <p className="text-xs font-heading font-medium text-muted-foreground">
        {label}
      </p>
      <p className="text-sm text-foreground">
        {value}
      </p>
    </div>
  );
}

function ChangeHistoryTimelineItemGroup({ type, records = [] }: {
  type: PropertyTransitionType,
  records?: Array<ChangeHistoryRecord>,
}) {
  if (records.length === 0) {
    return null;
  }

  return (
    <li key={type}>
      <PropertyTransitionTypeLabel type={type} />

      {records.map(record => (
        <ChangeHistoryTimelineItem key={record.name} record={record} />
      ))}
    </li>
  );
}

function ChangeHistoryTimelineItem({ record }: { record: ChangeHistoryRecord }) {
  return (
    <div className="flex flex-col text-xs">
      <PropertyName
        value={record.name}
        className="text-xs text-foreground/90 leading-loose overflow-hidden text-ellipsis whitespace-nowrap"
        title={record.name}
      />

      {record.action === PropertyTransitionType.ADDED && (
        <ChangeHistoryPropertyValue
          variant="addition"
          value={record.to}
        />
      )}

      {record.action === PropertyTransitionType.UPDATED && (
        <span className="overflow-hidden border rounded-md">
          <ChangeHistoryPropertyValue
            className="border-b border-border/50"
            variant="removal"
            value={record.from}
          />
          <ChangeHistoryPropertyValue
            variant="addition"
            value={record.to}
          />
        </span>
      )}

      {record.action === PropertyTransitionType.REMOVED && (
        <ChangeHistoryPropertyValue
          variant="removal"
          value={record.from}
        />
      )}
    </div>
  );
}

const TRANSITIONS = Object.values(PropertyTransitionType);

function useGroupedHistoryRecords(records: Array<ChangeHistoryRecord>) {
  return useMemo(
    () => records.reduce((state, record) => {
      const { action } = record;
      if (!state[action]) {
        state[action] = [];
      }
      state[action].push(record);
      return state;
    }, {} as Record<PropertyTransitionType, Array<ChangeHistoryRecord> | undefined>),
    [records],
  );
}

function ChangeHistoryTimeline({ history, namespace, service, profile }: {
  history: ChangeHistory,
  namespace: Namespace,
  service: Service,
  profile: Profile,
}) {
  const { data, error, isError, isPending } = useGetChangeHistoryDetails(namespace, service, profile, history);
  const records = useGroupedHistoryRecords(data || []);

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  if (isPending) {
    return (
      <ChangeHistorySkeleton />
    );
  }

  if (data.length === 0) {
    return (
      <EmptyState
        icon={<GalleryVerticalEndIcon />}
        title={
          <FormattedMessage
            defaultMessage="No property changes found"
            description="Empty state title used when no changes are found for a change history record."
          />
        }
      />
    );
  }

  return (
    <ul className="space-y-4">
      {TRANSITIONS.map((type) => (
        <ChangeHistoryTimelineItemGroup
          key={type}
          type={type}
          records={records[type]}
        />
      ))}
    </ul>
  );
}

export function ChangeHistorySidebar({
  open = false,
  onOpenChange,
  history,
  namespace,
  service,
  profile,
}: ChangeHistorySidebar) {
  if (!history) {
    return null;
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-lg! h-screenflex flex-col gap-0">
        <SheetHeader className="sticky top-0 border-b">
          <SheetTitle>
            {history.subject}
          </SheetTitle>
          <p>
            <code className="text-xs font-mono text-foreground/80 bg-muted px-2 py-1 rounded">
              {history.revision}
            </code>
          </p>
        </SheetHeader>

        <ScrollArea className="flex-1 overflow-auto">
          <div className="p-4 space-y-3">
            <ChangeHistoryStat
              label={
                <FormattedMessage
                  defaultMessage="Author"
                  description="Label for the author of a change history record"
                />
              }
              value={history.appliedBy}
            />

            <ChangeHistoryStat
              label={
                <FormattedMessage
                  defaultMessage="Timestamp"
                  description="Label for the applied at date of a change history record"
                />
              }
              value={<ChangeHistoryTimestamp history={history} />}
            />

            <ChangeHistoryStat
              label={
                <FormattedMessage
                  defaultMessage="Description"
                  description="Label for the description of a change history record"
                />
              }
              value={history.description ?? <MissingPropertyDescriptionLabel />}
            />

            <div className="flex justify-between items-center">
              <p className="text-xs font-heading font-medium text-muted-foreground">
                <FormattedMessage
                  defaultMessage="Property changes"
                  description="Label for the property changes of a change history record"
                />
              </p>
              <Badge variant="outline" className="text-xs">
                <ChangesCountLabel count={history.count} />
              </Badge>
            </div>

            <ChangeHistoryTimeline
              history={history}
              namespace={namespace}
              service={service}
              profile={profile}
            />
          </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
}
