'use client';

import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
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
import { toast } from '@konfigyr/components/ui/toast';
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import { useRemoveNamespaceApplication, useResetNamespaceApplication } from '@konfigyr/hooks';
import { DeleteNamespaceApplicationLabel } from '@konfigyr/components/namespace/applications/messages';
import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

type Props = {
  namespace: Namespace,
  application: NamespaceApplication,
  disabled?: boolean,
  onConfirm: (app: NamespaceApplication) => void | Promise<void>,
};

export function ConfirmNamespaceApplicationDeleteAction ({ namespace, application, onConfirm }: Props) {
  const errorNotification = useErrorNotification();

  const {
    isPending: isPending,
    mutateAsync: removeNamespaceApplication,
  } = useRemoveNamespaceApplication(namespace.slug);

  const onClickConfirm = useCallback(async () => {
    try {
      await removeNamespaceApplication(application.id);
      await onConfirm(application);
    } catch (error) {
      return errorNotification(error);
    }

    toast.add({
      type: 'success',
      title: (
        <FormattedMessage
          defaultMessage="The {name} was successfully deleted."
          description="Success message for deleting of an application"
          values={{ name: <strong>{application.name}</strong> }}
        />
      ),
    });
  }, [application, errorNotification]);

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant="destructive">
            <DeleteNamespaceApplicationLabel/>
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Delete {name} application"
              values={{
                name: <strong>{application.name}</strong>,
              }}
              description="Title of the modal that is shown when user tries todelete application"
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to delete {name} application? This action cannot be undone."
              values={{
                name: <strong>{application.name}</strong>,
              }}
              description="Confirmation text in the modal that is shown when user tries to delete a namespace application"
            />
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction onClick={onClickConfirm} disabled={isPending}>
            <YesLabel/>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function ConfirmNamespaceApplicationResetAction ({
  namespace,
  application,
  disabled = false,
  onConfirm,
}: Props) {
  const errorNotification = useErrorNotification();

  const {
    isPending,
    mutateAsync: resetApplication,
  } = useResetNamespaceApplication(namespace.slug);

  const onClickConfirm = useCallback(async () => {
    try {
      const created = await resetApplication(application.id);
      await onConfirm(created);
    } catch (error) {
      return errorNotification(error);
    }

    toast.add({
      type: 'success',
      title: (
        <FormattedMessage
          defaultMessage="The {name} application was successfully reset."
          description="Success message for resetting of an application"
          values={{ name: application.name }}
        />
      ),
    });
  }, [application, errorNotification]);

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant="outline" disabled={disabled}>
            <FormattedMessage
              defaultMessage="Reset application"
              description="Button label that triggers application reset confirmation dialog when clicked"
            />
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Reset {name} application"
              values={{
                name: <strong>{application.name}</strong>,
              }}
              description="Title of the modal that is shown when user tries to reset application"
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to reset {name} application? This action cannot be undone."
              values={{
                name: <strong>{application.name}</strong>,
              }}
              description="Confirmation text in the modal that is shown when user tries to rest a namespace application"
            />
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction onClick={onClickConfirm} disabled={isPending}>
            <YesLabel/>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
