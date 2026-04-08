'use client';

import { useCallback, useMemo } from 'react';
import { DotIcon, FileStackIcon, GitCommitIcon } from 'lucide-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { ClipboardIconButton } from '@konfigyr/components/clipboard';
import { RelativeDate } from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import { ChangesCountLabel } from '@konfigyr/components/vault/messages';
import { Button } from '@konfigyr/components/ui/button';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { Tooltip, TooltipContent, TooltipTrigger } from '@konfigyr/components/ui/tooltip';

import type { ChangeHistory } from '@konfigyr/hooks/vault/types';

type SelectChangeHistoryFn = ((item: ChangeHistory) => void);

export interface ChangeHistoryTimelineProps {
  history?: Array<ChangeHistory>,
  isPending?: boolean,
  error?: Error | null,
  onSelect?: SelectChangeHistoryFn,
}

function useGroupByDate(history: Array<ChangeHistory>): Record<string, Array<ChangeHistory>> {
  const intl = useIntl();

  return useMemo(
    () => history.reduce((state, record) => {
      const label = intl.formatDate(record.appliedAt, {
        year: 'numeric', month: 'long', day: 'numeric',
      });

      const records = state[label] ?? [];
      return { ...state, [label]: [...records, record] };
    }, {} as Record<string, Array<ChangeHistory>>),
    [history, intl],
  );
}

export function ChangeHistoryTimeline({ history, error, isPending, onSelect }: ChangeHistoryTimelineProps) {
  const groupedByDate = useGroupByDate(history ?? []);

  if (isPending) {
    return (
      <ChangeHistorySkeleton />
    );
  }

  if (error) {
    return (
      <ErrorState error={error} />
    );
  }

  if (history?.length === 0) {
    return (
      <EmptyState
        icon={<FileStackIcon />}
        title={
          <FormattedMessage
            defaultMessage="No history found for this profile."
            description="Empty state title used when no history is found for a profile."
          />
        }
      />
    );
  }

  return (
    <div className="grid gap-3 relative ml-3">
      <div className="absolute top-2 bottom-0 left-0 w-px border-l-2" />
      {Object.keys(groupedByDate).map((date) => (
        <ChangeHistory
          key={date}
          date={date}
          items={groupedByDate[date]}
          onSelect={onSelect}
        />
      ))}
    </div>
  );
}

function ChangeHistorySkeleton() {
  return (
    <div data-slot="changeset-history-skeleton" className="w-full space-y-4">
      <div className="flex items-center gap-2">
        <Skeleton className="size-4 rounded-full" />
        <Skeleton className="h-4 w-32" />
      </div>

      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="flex items-start justify-between gap-4 ml-6 px-2">
          <div className="flex flex-col gap-2 min-w-0 flex-1">
            <Skeleton className="h-4 w-64" />
            <Skeleton className="h-3 w-48" />
          </div>
          <div className="flex flex-col gap-2 items-end">
            <Skeleton className="h-3 w-72" />
            <Skeleton className="h-3 w-24" />
          </div>
        </div>
      ))}
    </div>
  );
}

function ChangeHistory({ date, items, onSelect }: { date: string, items: Array<ChangeHistory>, onSelect?: SelectChangeHistoryFn }) {
  return (
    <div className="relative flex pl-6 gap-3">
      <div className="absolute top-2 left-px size-6 -translate-x-1/2 rounded-full bg-background">
        <GitCommitIcon className="size-6 text-foreground/50" />
      </div>

      <div className="flex-1">
        <div className="py-2 h-6 font-heading text-sm text-foreground/80 mb-3">
          {date}
        </div>
        <ItemGroup size="xs" className="border rounded-lg px-3 py-1">
          {items.map(artifact => (
            <ChangeHistoryItem key={artifact.id} item={artifact} onSelect={onSelect}/>
          ))}
        </ItemGroup>
      </div>
    </div>
  );
}

function ChangeHistoryItem({ item, onSelect }: { item: ChangeHistory, onSelect?: SelectChangeHistoryFn }) {
  const onClick = useCallback(() => onSelect?.(item), [item, onSelect]);

  return (
    <Item variant="list" size="xs" role='listitem' aria-label={item.subject}>
      <ItemContent>
        <ItemTitle>
          {item.subject}
        </ItemTitle>
        <ItemDescription className="flex items-center gap-0">
          <span>{item.appliedBy}</span>
          <DotIcon />
          <ChangesCountLabel count={item.count} />
          <DotIcon />
          <RelativeDate value={item.appliedAt} />
        </ItemDescription>
      </ItemContent>
      <ItemActions>
        <Tooltip>
          <TooltipTrigger
            render={
              <Button variant="outline" size="xs" onClick={onClick}>
                <code className="w-18 truncate">{item.revision}</code>
              </Button>
            }
          />
          <TooltipContent>
            <FormattedMessage
              defaultMessage="View change details for this revision"
              description="Tooltip message when clicking on a revision in the change history."
            />
          </TooltipContent>
        </Tooltip>

        <ClipboardIconButton
          text={item.revision}
          variant="outline"
          size="xs"
          tooltip={
            <FormattedMessage
              defaultMessage="Copy full revision hash to clipboard"
              description="Tooltip message when copying the full change history revision hash."
            />
          }
        />
      </ItemActions>
    </Item>
  );
}


