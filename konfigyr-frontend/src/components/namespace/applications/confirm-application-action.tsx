'use client';

import { toast } from 'sonner';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
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

import type { NamespaceApplication } from '@konfigyr/hooks/types';

type Props = {
  application?: NamespaceApplication | null
  isPending: boolean,
  onClose: () => void
  onConfirm: (id: string) => void
};

export function ConfirmNamespaceApplicationDeleteAction({ application, isPending,  onClose = () => {}, onConfirm = () => {} }: Props) {
  const open = useMemo(() => !!application , [application]);
  const errorNotification = useErrorNotification();

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onClickConfirm = useCallback(async () => {
    try {
      await onConfirm(application!.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<FormattedMessage
      defaultMessage="The {name} wassuccessfully deleted."
      values={{ name: application?.name }}
      description="Success message for deleting of an application"
    />);

    return onClose();
  }, [application, onClose, errorNotification]);

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Delete {name} application"
              values={{ name: application?.name }}
              description="Title of the modal that is shown when user tries todelete application"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are you sure you want to delete {name} application? This action cannot be undone."
            values={{ name: application?.name }}
            description="Confirmation text in the modal that is shown when user tries to delete a namespace application"
          />
        </AlertDialogDescription>
        <AlertDialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <FormattedMessage
              defaultMessage="Cancel"
              description="Label for the cancel button in the modal"
            />
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            loading={isPending}
            onClick={onClickConfirm}
          >
            <FormattedMessage
              defaultMessage="Yes"
              description="Label for the confirm button in the modal"
            />
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function ConfirmNamespaceApplicationResetAction({ application, isPending, onClose = () => {}, onConfirm = () => {} }: Props) {
  const open = useMemo(() => !!application , [application]);
  const errorNotification = useErrorNotification();

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onClickConfirm = useCallback(async () => {
    try {
      await onConfirm(application!.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<FormattedMessage
      defaultMessage="The {name} application was successfully reset."
      values={{ name: application?.name }}
      description="Success message for resetting of an application"
    />);

    return onClose();
  }, [application, onClose, errorNotification]);

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Reset {name}"
              values={{ name: application?.name }}
              description="Title of the modal that is shown when user tries to reset application"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are you sure you want to rest {name} application? This action cannot be undone."
            values={{ name: application?.name }}
            description="Confirmation text in the modal that is shown when user tries to rest a namespace application"
          />
        </AlertDialogDescription>
        <AlertDialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <FormattedMessage
              defaultMessage="Cancel"
              description="Label for the cancel button in the modal"
            />
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            loading={isPending}
            onClick={onClickConfirm}
          >
            <FormattedMessage
              defaultMessage="Yes"
              description="Label for the confirm button in the modal"
            />
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
