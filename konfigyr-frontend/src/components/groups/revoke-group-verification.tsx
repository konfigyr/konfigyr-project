'use client';

import { toast } from 'sonner';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useRevokeGroupVerification } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@konfigyr/components/ui/alert-dialog';
import { Button } from '@konfigyr/components/ui/button';
import { CancelLabel } from '@konfigyr/components/messages';
import { TriangleAlert } from 'lucide-react';
import type { GroupVerification } from '@konfigyr/hooks/types';

export function RevokeGroupVerificationButton({
  namespace,
  verification,
}: {
  namespace: string;
  verification: GroupVerification;
}) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: revokeGroupVerification } = useRevokeGroupVerification(namespace);

  const onRevoke = useCallback(async () => {
    try {
      await revokeGroupVerification(verification.groupId);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(
      <FormattedMessage
        defaultMessage="Successfully revoked {groupId}"
        values={{ groupId: verification.groupId }}
        description="Success message when a group verification claim is revoked"
      />,
    );
  }, [verification, errorNotification, revokeGroupVerification]);

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button size="sm" variant="destructive">
            <FormattedMessage
              defaultMessage="Revoke"
              description="Label of the button that revokes a group verification claim."
            />
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            <TriangleAlert className="h-5 w-5 text-destructive shrink-0" />
            <FormattedMessage
              defaultMessage="Revoke group verification claim"
              description="Title of the confirmation dialog for revoking a group verification claim"
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to revoke the group verification claim for <b>{groupId}</b>?"
              values={{
                groupId: verification.groupId,
                b: (chunks) => <strong>{chunks}</strong>,
              }}
              description="Confirmation text in the dialog for revoking a group verification claim"
            />
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel />
          </AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={isPending} loading={isPending} onClick={onRevoke}>
            <FormattedMessage
              defaultMessage="Revoke claim"
              description="Label for the confirm button in the dialog for revoking a group verification claim"
            />
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
