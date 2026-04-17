import { FormattedMessage, useIntl } from 'react-intl';
import {
  PencilIcon,
} from 'lucide-react';
import {
  useDiscardChangeRequest,
  useGetChangeRequest,
  useGetChangeRequestChanges,
  useGetChangeRequestHistory,
  useMergeChangeRequest,
  useReviewChangeRequest,
  useUpdateChangeRequest,
} from '@konfigyr/hooks';
import {
  ChangeRequestState,
  PropertyTransitionType,
} from '@konfigyr/hooks/vault/types';
import { ErrorState } from '@konfigyr/components/error';
import { RelativeDate } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  InlineEdit,
  InlineEditInput,
  InlineEditPlaceholder,
} from '@konfigyr/components/ui/inline-edit';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  PropertyTransitionItemGroup,
  PropertyTransitionItemSkeleton,
  useGroupedPropertyTransitions,
} from '../change-history/property-transition';
import { ChangeRequestMergeStateCard } from './change-request-merge-state';
import { ChangeRequestStateBadge } from './change-request-state';
import { ChangeRequestStateSummary } from './change-request-state-summary';
import { ChangeRequestSubmitReview } from './change-request-submit-review';
import { ChangeRequestTimeline } from './change-request-timeline';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeRequest, ChangeRequestHistory } from '@konfigyr/hooks/vault/types';

function ChangeRequestSubject({ changeRequest, onChange }: {
  changeRequest: ChangeRequest;
  onChange: (subject?: string) => void;
}) {
  const intl = useIntl();

  return (
    <InlineEdit
      value={changeRequest.subject}
      onChange={onChange}
    >
      <InlineEditPlaceholder
        render={(
          <span className="flex items-center gap-1">
            <span className="font-heading font-medium">
              {changeRequest.subject}
            </span>
            <Button variant="ghost" size="icon-sm" className="cursor-pointer">
              <PencilIcon />
            </Button>
          </span>
        )}
      />
      <InlineEditInput
        className="h-7 text-xs min-w-56 max-w-full"
        aria-label={intl.formatMessage({
          defaultMessage: 'Edit change request subject',
          description: 'Change request subject inline edit label.',
        })}
      />
    </InlineEdit>
  );
}

const TRANSITIONS = Object.values(PropertyTransitionType);

function ChangeRequestChanges({ namespace, service, changeRequest }: {
  namespace: Namespace;
  service: Service;
  changeRequest: ChangeRequest;
}) {
  const { data, error, isError, isPending } = useGetChangeRequestChanges(namespace, service, changeRequest.number);
  const changes = useGroupedPropertyTransitions(data?.data || []);

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle>
          <FormattedMessage
            defaultMessage="Property changes"
            description="Title of the property changes section in the change request details page."
          />
        </CardTitle>
        <CardAction>
          <RelativeDate value={changeRequest.createdAt} />
        </CardAction>
      </CardHeader>
      <CardContent>
        {isPending && (
          <PropertyTransitionItemSkeleton />
        )}

        {isError && (
          <ErrorState error={error} />
        )}

        <ul className="space-y-4">
          {TRANSITIONS.map((type) => (
            <li key={type}>
              <PropertyTransitionItemGroup
                type={type}
                transitions={changes[type]}
              />
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

function ChangeRequestHistoryItemSkeleton() {
  return (
    <div data-slot="timeline-item-skeleton" className="relative flex gap-3">
      <Skeleton className="size-8 rounded-full" />
      <div className="flex-1 pb-6 min-w-0">
        <Skeleton className="h-4 w-56 my-2 shrink-0" />
        <Skeleton className="h-12" />
      </div>
    </div>
  );
}

function ChangeRequestHistory({ namespace, service, changeRequest }: {
  namespace: Namespace;
  service: Service;
  changeRequest: ChangeRequest;
}) {
  const { data, error, isError, isPending } = useGetChangeRequestHistory(namespace, service, changeRequest.number);

  if (isPending) {
    return (
      <ChangeRequestHistoryItemSkeleton />
    );
  }

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  return (
    <ChangeRequestTimeline history={data.data} />
  );
}

function ChangeRequestDetailsSkeleton() {
  return (
    <div data-slot="change-request-details-skeleton" className="space-y-6">
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          <Skeleton className="size-8" />
          <Skeleton className="h-8 w-64" />
        </div>
        <div className="flex items-center gap-2">
          <Skeleton className="h-6 w-16 rounded-4xl" />
          <Skeleton className="h-5 w-92" />
        </div>
      </div>
      <PropertyTransitionItemSkeleton />
      <ChangeRequestHistoryItemSkeleton />
    </div>
  );
}

function ChangeRequestDetailsArticle({ namespace, service, changeRequest }: {
  namespace: Namespace;
  service: Service;
  changeRequest: ChangeRequest;
}) {
  const { mutateAsync: updateChangeRequest } = useUpdateChangeRequest(namespace, service, changeRequest);
  const { mutateAsync: mergeChangeRequest } = useMergeChangeRequest(namespace, service, changeRequest);
  const { mutateAsync: reviewChangeRequest } = useReviewChangeRequest(namespace, service, changeRequest);
  const { mutateAsync: discardChangeRequest } = useDiscardChangeRequest(namespace, service, changeRequest);

  return (
    <article className="space-y-4">
      <header className="space-y-2">
        <p className="text-2xl flex items-center gap-1">
          <span className="text-foreground/70">#{changeRequest.number}</span>
          <ChangeRequestSubject
            changeRequest={changeRequest}
            onChange={subject => updateChangeRequest({ subject })}
          />
        </p>
        <p className="flex items-center gap-1">
          <ChangeRequestStateBadge value={changeRequest.state} />
          <span className="text-sm">
            <ChangeRequestStateSummary changeRequest={changeRequest} />
          </span>
        </p>
      </header>

      <div className="space-y-4">
        <ChangeRequestChanges
          namespace={namespace}
          service={service}
          changeRequest={changeRequest}
        />
        <ChangeRequestHistory
          namespace={namespace}
          service={service}
          changeRequest={changeRequest}
        />
      </div>

      {changeRequest.state === ChangeRequestState.OPEN && (
        <footer className="space-y-6">
          <ChangeRequestMergeStateCard
            value={changeRequest.mergeStatus}
            onMerge={mergeChangeRequest}
          />
          <ChangeRequestSubmitReview
            onDiscard={() => discardChangeRequest()}
            onReview={review => reviewChangeRequest(review)}
          />
        </footer>
      )}
    </article>
  );
}

export function ChangeRequestDetails({ namespace, service, number }: {
  namespace: Namespace;
  service: Service;
  number: string | number;
}) {
  const { data, isLoading, isError, error } = useGetChangeRequest(namespace, service, number);

  if (isLoading) {
    return (
      <ChangeRequestDetailsSkeleton />
    );
  }

  if (isError) {
    return (
      <ErrorState error={error} />
    );
  }

  return (
    <ChangeRequestDetailsArticle
      namespace={namespace}
      service={service}
      changeRequest={data!}
    />
  );
}
