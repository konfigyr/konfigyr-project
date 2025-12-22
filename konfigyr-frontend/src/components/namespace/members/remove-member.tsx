'use client';

import { toast } from 'sonner';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { useRemoveNamespaceMember } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@konfigyr/components/ui/alert-dialog';

import type { Member, Namespace } from '@konfigyr/hooks/types';

export function RemoveMemberForm({ namespace, member, onClose }: { namespace: Namespace, member?: Member | null, onClose: () => void }) {
  const open = useMemo(() => !!member , [member]);

  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: removeNamespaceMember } = useRemoveNamespaceMember(namespace.slug);

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onRemove = useCallback(async () => {
    try {
      await removeNamespaceMember(member!.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<FormattedMessage
      defaultMessage="Successfully removed {name} from {namespace}"
      values={{ name: member?.fullName, namespace: namespace.name }}
      description="Success message when member is removed from a namespace"
    />);

    return onClose();
  }, [member, onClose, errorNotification]);

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Removing namespace member"
              description="Title of the modal that is shown when user tries to remove a namespace member"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are your sure you want to remove {name} from your namespace?"
            values={{ name: member?.fullName }}
            description="Confirmation text in the modal that is shown when user tries to remove a namespace member"
          />
        </AlertDialogDescription>
        <AlertDialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <FormattedMessage
              defaultMessage="Cancel"
              description="Label for the cancel button in the modal that is shown when user tries to remove a namespace member"
            />
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            loading={isPending}
            onClick={onRemove}
          >
            <FormattedMessage
              defaultMessage="Yes, remove"
              description="Label for the confirm button in the modal that is shown when user tries to remove a namespace member"
            />
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
