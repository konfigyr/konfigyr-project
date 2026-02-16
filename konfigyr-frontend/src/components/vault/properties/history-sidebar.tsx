'use client';

import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  FileStackIcon,
  PencilIcon,
  PlusIcon,
  TrashIcon,
  UserIcon,
} from 'lucide-react';
import { useGetHistory } from '@konfigyr/hooks';
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
import { ErrorState } from '@konfigyr/components/error';
import { cn } from '@konfigyr/components/utils';
import { PropertyDescription } from './property-description';
import { PropertyName } from './property-name';
import { StateBadge } from './state-label';
import { PropertyValueLabel } from './messages';

import type { ChangeHistoryRecord, ConfigurationProperty, Profile } from '@konfigyr/hooks/types';

const actionConfig: Record<ChangeHistoryRecord['action'], {
  icon: React.FC<{ className?: string }>
  state: 'added' | 'modified' | 'deleted'
  color: string
}> = {
  created: {
    icon: PlusIcon,
    state: 'added',
    color: 'bg-emerald-500',
  },
  modified: {
    icon: PencilIcon,
    state: 'modified',
    color: 'bg-amber-500',
  },
  deleted: {
    icon: TrashIcon,
    state: 'deleted',
    color: 'bg-destructive',
  },
};

function TimelineItem({ record, isLast }: { record: ChangeHistoryRecord, isLast: boolean }) {
  const config = actionConfig[record.action];
  const Icon = config.icon;

  return (
    <li className="relative flex gap-3">
      {!isLast && (
        <div className="absolute left-2.25 top-4 bottom-0 w-px bg-border" />
      )}

      <div className="relative flex shrink-0">
        <div className={cn('size-5 rounded-full flex items-center justify-center', config.color)}>
          <Icon className="size-2.5 text-background" />
        </div>
      </div>

      <div className="flex-1 pb-6 min-w-0">
        <div className="flex items-baseline justify-between gap-2">
          <StateBadge variant={config.state} size="sm" />
          <RelativeDate
            value={record.timestamp}
            className="text-xs text-muted-foreground/60 font-mono shrink-0 tabular-nums"
          />
        </div>

        <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
          <UserIcon className="size-3" />
          <span className="truncate">{record.user}</span>
        </div>

        {(record.previousValue || record.newValue) && (
          <div className="mt-2 rounded-md border bg-muted/30 overflow-hidden text-xs font-mono">
            {record.previousValue && (
              <div className="flex items-start gap-2 px-3 py-1 bg-destructive/5 border-b border-border/50">
                <span className="text-destructive/60 select-none shrink-0">-</span>
                <span className="text-destructive/80 break-all">
                  {record.previousValue}
                </span>
              </div>
            )}
            {record.newValue && (
              <div className="flex items-start gap-2 px-3 py-1 bg-emerald-500/5">
                <span className="text-emerald-600/60 dark:text-emerald-400/60 select-none shrink-0">+</span>
                <span className="text-emerald-700 dark:text-emerald-300 break-all">
                  {record.newValue}
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

function Timeline({ profile, property }: { profile: Profile, property: ConfigurationProperty }) {
  const { data: history, error, isLoading, isError } = useGetHistory(profile, property.name);


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

  if (!history || history.length === 0) {
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
      {history.map((record, index) => (
        <TimelineItem
          key={record.id}
          record={record}
          isLast={index === history.length - 1}
        />
      ))}
    </ul>
  );
}

interface PropertyHistorySidebarProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  property: ConfigurationProperty | null;
  profile: Profile;
}

export function PropertyHistorySidebar({
  open,
  onOpenChange,
  property,
  profile,
}: PropertyHistorySidebarProps) {
  if (!property) {
    return null;
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-md overflow-y-auto p-0">
        <div className="sticky top-0 z-10 bg-background border-b px-6 pt-6 pb-4">
          <SheetHeader className="gap-1">
            <SheetTitle className="text-base font-semibold">
              <FormattedMessage
                defaultMessage="Property History"
                description="Title of the property history sheet"
              />
            </SheetTitle>
            <SheetDescription asChild>
              <div className="flex flex-col gap-1.5">
                <PropertyName
                  value={property.name}
                  className="font-medium text-foreground bg-muted px-2 py-1 rounded-md inline-block w-fit"
                />
                <PropertyDescription
                  value={property.description}
                />
              </div>
            </SheetDescription>
          </SheetHeader>
        </div>

        <div className="px-6 py-4 border-b bg-muted/20">
          <div className="text-xs uppercase tracking-wider text-muted-foreground/60 font-medium">
            <PropertyValueLabel />
          </div>
          <div className="font-mono text-sm text-foreground my-2">
            {property.value}
          </div>
          <div className="text-xs text-muted-foreground/50 font-mono">
            {property.type}
          </div>
        </div>

        <div className="px-6 py-5">
          <Timeline profile={profile} property={property} />
        </div>
      </SheetContent>
    </Sheet>
  );
}
