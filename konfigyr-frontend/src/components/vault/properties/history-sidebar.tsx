'use client';

import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { FileStackIcon, GitCommitIcon, UserIcon } from 'lucide-react';
import { useGetPropertyHistory } from '@konfigyr/hooks';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { RelativeDate } from '@konfigyr/components/messages';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@konfigyr/components/ui/sheet';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { ScrollArea } from '@konfigyr/components/ui/scroll-area';
import { ErrorState } from '@konfigyr/components/error';
import { useLabelForTransitionType } from '@konfigyr/components/vault/messages';
import { PropertyTransitionTypeLabel } from '@konfigyr/components/vault/change-history/property-transition-type';
import { PropertyValueLabel } from './messages';

import type {
  ChangeHistoryRecord,
  ConfigurationProperty,
  Namespace,
  Profile,
  Service,
} from '@konfigyr/hooks/types';

function TimelineItem({ record, isLast }: { record: ChangeHistoryRecord, isLast: boolean }) {
  const label = useLabelForTransitionType(record.action);

  return (
    <li className="relative flex gap-3">
      {!isLast && (
        <div className="absolute left-2.25 top-4 bottom-0 w-px bg-border" />
      )}

      <PropertyTransitionTypeLabel
        type={record.action}
        variant="icon"
        className="relative flex shrink-0"
      />

      <div className="flex-1 pb-6 min-w-0">
        <div className="flex items-baseline justify-between gap-2 text-xs">
          {label}
          <RelativeDate
            value={record.appliedAt}
            className="text-muted-foreground/60 shrink-0 tabular-nums"
          />
        </div>

        <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
          <GitCommitIcon className="size-3" />
          <span className="truncate">{record.revision}</span>
        </div>

        <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
          <UserIcon className="size-3" />
          <span className="truncate">{record.appliedBy}</span>
        </div>

        {(record.from || record.to) && (
          <div className="mt-2 rounded-md border bg-muted/30 overflow-hidden text-xs font-mono">
            {record.from && (
              <div className="flex items-start gap-2 px-3 py-1 bg-destructive/5 border-b border-border/50">
                <span className="text-destructive/60 select-none shrink-0">-</span>
                <span className="text-destructive/80 break-all">
                  {record.from}
                </span>
              </div>
            )}
            {record.to && (
              <div className="flex items-start gap-2 px-3 py-1 bg-emerald-500/5">
                <span className="text-emerald-600/60 dark:text-emerald-400/60 select-none shrink-0">+</span>
                <span className="text-emerald-700 dark:text-emerald-300 break-all">
                  {record.to}
                </span>
              </div>
            )}
          </div>
        )}
      </div>
    </li>
  );
}

function TimelineItemSkeleton() {
  return (
    <div data-slot="timeline-item-skeleton" className="relative flex gap-3">
      <Skeleton className="size-5 rounded-full" />
      <div className="flex-1 pb-6 min-w-0">
        <div className="flex items-baseline justify-between gap-2">
          <Skeleton className="h-4 w-12 shrink-0" />
          <Skeleton className="h-3 w-24 shrink-0" />
        </div>
        <Skeleton className="h-3 w-36 my-2 shrink-0" />
        <Skeleton className="h-12" />
      </div>
    </div>
  );
}

function Timeline<T>({ namespace, service, profile, property }: {
  namespace: Namespace,
  service: Service,
  profile: Profile,
  property: ConfigurationProperty<T>
}) {
  const { data: history, error, isLoading, isError } = useGetPropertyHistory(namespace, service, profile, property.name);

  if (isLoading) {
    return (
      <TimelineItemSkeleton />
    );
  }

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  if (!history?.data || history.data.length === 0) {
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
    <ul>
      {history.data.map((record, index) => (
        <TimelineItem
          key={record.id}
          record={record}
          isLast={index === history.data.length - 1}
        />
      ))}
    </ul>
  );
}

interface PropertyHistorySidebarProps<T> {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  property: ConfigurationProperty<T> | null;
  namespace: Namespace;
  service: Service;
  profile: Profile;
}

export function PropertyHistorySidebar<T>({
  open,
  onOpenChange,
  property,
  namespace,
  service,
  profile,
}: PropertyHistorySidebarProps<T>) {
  if (!property) {
    return null;
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-md overflow-y-auto p-0 gap-0">
        <SheetHeader className="sticky top-0 gap-1 border-b">
          <SheetTitle className="text-base font-semibold">
            <FormattedMessage
              defaultMessage="Property History"
              description="Title of the property history sheet"
            />
          </SheetTitle>
          <SheetDescription className="flex flex-col gap-1.5">
            <PropertyName
              value={property.name}
              className="font-medium text-foreground bg-muted px-2 py-1 rounded-md inline-block w-fit"
            />
            <PropertyDescription
              value={property.description}
            />
          </SheetDescription>
        </SheetHeader>

        <ScrollArea className="flex-1 overflow-auto">
          <div className="p-4 border-b bg-muted/20">
            <div className="text-xs uppercase tracking-wider text-muted-foreground/60 font-medium">
              <PropertyValueLabel />
            </div>
            <div className="font-mono text-sm text-foreground my-2">
              {property.value?.encoded}
            </div>
            <div className="text-xs text-muted-foreground/50 font-mono">
              {property.typeName}
            </div>
          </div>

          <div className="p-4">
            <Timeline
              namespace={namespace}
              service={service}
              profile={profile}
              property={property}
            />
          </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
}
