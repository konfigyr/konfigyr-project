'use client';

import { toast } from 'sonner';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import {useRemoveNamespaceApplication, useResetNamespaceApplication} from '@konfigyr/hooks';
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

import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

type Props = {
  action: string,
  isPending: boolean,
  application?: NamespaceApplication | null
  onClose: () => void
  onConfirm: (id: string) => void
};

export function ConfirmNamespaceApplicationAction({ application, isPending, action, onClose = () => {}, onConfirm = () => {} }: Props) {
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
      defaultMessage="The {action} action completed successfully."
      values={{ action: action }}
      description="Success message"
    />);

    return onClose();
  }, [application, onClose, errorNotification]);

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Confirm {action}"
              values={{ action: action }}
              description="Title of the modal that is shown when user tries to confirm action"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are you sure you want to {action} this application? This action cannot be undone."
            values={{ action: action }}
            description="Confirmation text in the modal that is shown when user tries to remove a namespace application"
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
