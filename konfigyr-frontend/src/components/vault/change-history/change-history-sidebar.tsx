'use client';

import { FormattedDate, FormattedMessage } from 'react-intl';
import { GalleryVerticalEndIcon } from 'lucide-react';
import { useGetChangeHistoryDetails } from '@konfigyr/hooks';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { ErrorState } from '@konfigyr/components/error';
import { MissingPropertyDescriptionLabel } from '@konfigyr/components/artifactory/messages';
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
import { ScrollArea } from '@konfigyr/components/ui/scroll-area';
import {
  PropertyTransitionItemGroup,
  PropertyTransitionItemSkeleton,
  useGroupedPropertyTransitions,
} from './property-transition';

import type { ReactNode } from 'react';
import type {
  ChangeHistory,
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

const TRANSITIONS = Object.values(PropertyTransitionType);

function ChangeHistoryTimeline({ history, namespace, service, profile }: {
  history: ChangeHistory,
  namespace: Namespace,
  service: Service,
  profile: Profile,
}) {
  const { data, error, isError, isPending } = useGetChangeHistoryDetails(namespace, service, profile, history);
  const records = useGroupedPropertyTransitions(data || []);

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  if (isPending) {
    return (
      <PropertyTransitionItemSkeleton />
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
        <li key={type}>
          <PropertyTransitionItemGroup
            type={type}
            transitions={records[type]}
          />
        </li>
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
