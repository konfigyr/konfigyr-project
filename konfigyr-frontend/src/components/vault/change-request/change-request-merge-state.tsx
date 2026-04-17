import { toast } from 'sonner';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  CircleCheckIcon,
  CircleXIcon,
  GitCompareArrowsIcon,
  GitMergeConflictIcon,
  GitMergeIcon,
  GitPullRequestClosedIcon,
  MessageCirclePlusIcon,
} from 'lucide-react';
import { useErrorNotification } from '@konfigyr/components/error';
import { ChangeRequestMergeStatus } from '@konfigyr/hooks/vault/types';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { cn } from '@konfigyr/components/utils';

function ChangeRequestMergeStateIcon({ value, className }: { value: ChangeRequestMergeStatus, className?: string }) {
  switch (value) {
    case ChangeRequestMergeStatus.NOT_APPROVED:
      return (
        <CircleXIcon className={cn('text-destructive', className)} />
      );
    case ChangeRequestMergeStatus.NOT_OPEN:
      return (
        <GitPullRequestClosedIcon className={className} />
      );
    case ChangeRequestMergeStatus.CHANGES_REQUESTED:
      return (
        <MessageCirclePlusIcon className={cn('text-destructive', className)} />
      );
    case ChangeRequestMergeStatus.CHECKING:
      return (
        <GitCompareArrowsIcon className={className} />
      );
    case ChangeRequestMergeStatus.CONFLICTING:
    case ChangeRequestMergeStatus.OUTDATED:
      return (
        <GitMergeConflictIcon className={cn('text-destructive', className)} />
      );
    case ChangeRequestMergeStatus.MERGEABLE:
      return (
        <CircleCheckIcon className={cn('text-emerald-500', className)} />
      );
  }
}

function ChangeRequestMergeStateTitle({ value }: { value: ChangeRequestMergeStatus }) {
  switch (value) {
    case ChangeRequestMergeStatus.NOT_APPROVED:
      return (
        <FormattedMessage
          defaultMessage="Review required"
          description="Title of the change request merge card when change request is not approved."
        />
      );
    case ChangeRequestMergeStatus.NOT_OPEN:
      return (
        <FormattedMessage
          defaultMessage="Change request not open"
          description="Title of the change request merge card when change request is not open."
        />
      );
    case ChangeRequestMergeStatus.CHANGES_REQUESTED:
      return (
        <FormattedMessage
          defaultMessage="Additional changes required"
          description="Title of the change request merge card when change request has additional changes requested."
        />
      );
    case ChangeRequestMergeStatus.CHECKING:
      return (
        <FormattedMessage
          defaultMessage="Checking..."
          description="Title of the change request merge card when merge state is being checked."
        />
      );
    case ChangeRequestMergeStatus.CONFLICTING:
      return (
        <FormattedMessage
          defaultMessage="Conflicts detected"
          description="Title of the change request merge card when merge conflicts are detected."
        />
      );
    case ChangeRequestMergeStatus.MERGEABLE:
      return (
        <FormattedMessage
          defaultMessage="Ready to merge"
          description="Title of the change request merge card when change request is ready to be merged."
        />
      );
    case ChangeRequestMergeStatus.OUTDATED:
      return (
        <FormattedMessage
          defaultMessage="Change request outdated"
          description="Title of the change request merge card when change request is outdated."
        />
      );
  }
}

function ChangeRequestMergeStateDescription({ value }: { value: ChangeRequestMergeStatus }) {
  switch (value) {
    case ChangeRequestMergeStatus.NOT_APPROVED:
      return (
        <FormattedMessage
          defaultMessage="At least one review is required before the change request can be merged."
          description="Description of the change request merge card when change request is not approved."
        />
      );
    case ChangeRequestMergeStatus.NOT_OPEN:
      return (
        <FormattedMessage
          defaultMessage="Change request was either merged or discarded."
          description="Description of the change request merge card when change request is not open."
        />
      );
    case ChangeRequestMergeStatus.CHANGES_REQUESTED:
      return (
        <FormattedMessage
          defaultMessage="At least one reviewer requested additional changes before the change request can be merged."
          description="Description of the change request merge card when change request has additional changes requested."
        />
      );
    case ChangeRequestMergeStatus.CHECKING:
      return (
        <FormattedMessage
          defaultMessage="We are checking the mergeability of this change request. This may take a few minutes."
          description="Description of the change request merge card when merge state is being checked."
        />
      );
    case ChangeRequestMergeStatus.CONFLICTING:
      return (
        <FormattedMessage
          defaultMessage="The change request cannot be merged because of merge conflicts. Please clone this change request, resolve the conflicts and submit your changes again."
          description="Description of the change request merge card when merge conflicts are detected."
        />
      );
    case ChangeRequestMergeStatus.MERGEABLE:
      return (
        <FormattedMessage
          defaultMessage="Your change request is ready to be merged. Click the merge button to proceed."
          description="Description of the change request merge card when change request is ready to be merged."
        />
      );
    case ChangeRequestMergeStatus.OUTDATED:
      return (
        <FormattedMessage
          defaultMessage="The change request is outdated. Please rebase the changes from the target profile."
          description="Description of the change request merge card when change request is outdated."
        />
      );
  }
}

export function ChangeRequestMergeStateCard({ value, onMerge }: {
  value: ChangeRequestMergeStatus;
  onMerge: () => void | Promise<any>;
}) {
  const errorNotification = useErrorNotification();
  const onMergeChangeRequest = useCallback(async () => {
    try {
      await onMerge();
    } catch (error) {
      return errorNotification(error);
    }

    toast.success((
      <FormattedMessage
        defaultMessage="Change request was merged"
        description="Notification message shown when change request was merged."
      />
    ));
  }, [onMerge]);

  return (
    <Card className="border">
      <CardHeader className="flex gap-2">
        <ChangeRequestMergeStateIcon className="size-6" value={value} />
        <div>
          <CardTitle>
            <ChangeRequestMergeStateTitle value={value} />
          </CardTitle>
          <CardDescription>
            <ChangeRequestMergeStateDescription value={value} />
          </CardDescription>
        </div>
      </CardHeader>
      <CardFooter>
        <Button
          variant="outline"
          disabled={value !== ChangeRequestMergeStatus.MERGEABLE}
          onClick={onMergeChangeRequest}
        >
          <GitMergeIcon data-icon="inline-start" />
          <FormattedMessage
            defaultMessage="Merge changes"
            description="Button label that triggers merge of change request"
          />
        </Button>
      </CardFooter>
    </Card>
  );
}
