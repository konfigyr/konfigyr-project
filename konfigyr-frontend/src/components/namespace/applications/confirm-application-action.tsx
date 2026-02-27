'use client';

import { toast } from 'sonner';
import {useCallback, useMemo, useState} from 'react';
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
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import { useRemoveNamespaceApplication, useResetNamespaceApplication } from '@konfigyr/hooks';
import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

type Props = {
  namespace: Namespace,
  application?: NamespaceApplication | null,
  onClose: () => void,
  onSuccess: () => void
};

export function ConfirmNamespaceApplicationDeleteAction({ namespace, application, onClose = () => {}, onSuccess = () => {} }: Props) {
  const open = useMemo(() => !!application , [application]);
  const errorNotification = useErrorNotification();

  const {
    isPending: isPending,
    mutateAsync: removeNamespaceApplication,
  } = useRemoveNamespaceApplication(namespace.slug);

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onClickConfirm = useCallback(async () => {
    try {
      await removeNamespaceApplication(application!.id);
      onSuccess();
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<FormattedMessage
      defaultMessage="The &quot;{name}&quot; was successfully deleted."
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
              defaultMessage="Delete &quot;{name}&quot; application"
              values={{ name: application?.name }}
              description="Title of the modal that is shown when user tries todelete application"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are you sure you want to delete &quot;{name}&quot; application? This action cannot be undone."
            values={{ name: application?.name }}
            description="Confirmation text in the modal that is shown when user tries to delete a namespace application"
          />
        </AlertDialogDescription>
        <AlertDialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <CancelLabel />
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            loading={isPending}
            onClick={onClickConfirm}
          >
            <YesLabel />
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function ConfirmNamespaceApplicationResetAction({ namespace, application, onClose = () => {}, onSuccess = () => {} }: Props) {
  const open = useMemo(() => !!application , [application]);
  const errorNotification = useErrorNotification();

  const {
    isPending,
    mutateAsync: resetApplication,
  } = useResetNamespaceApplication(namespace.slug);


  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onClickConfirm = useCallback(async () => {
    try {
      await resetApplication(application!.id);
      onSuccess();
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
              defaultMessage="Reset &quot;{name}&quot; application"
              values={{ name: application?.name }}
              description="Title of the modal that is shown when user tries to reset application"
            />
          </AlertDialogTitle>
        </AlertDialogHeader>
        <AlertDialogDescription>
          <FormattedMessage
            defaultMessage="Are you sure you want to reset &quot;{name}&quot; application? This action cannot be undone."
            values={{ name: application?.name }}
            description="Confirmation text in the modal that is shown when user tries to rest a namespace application"
          />
        </AlertDialogDescription>
        <AlertDialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            <CancelLabel />
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            loading={isPending}
            onClick={onClickConfirm}
          >
            <YesLabel />
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
