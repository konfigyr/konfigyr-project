import { FormattedMessage } from 'react-intl';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@konfigyr/components/ui/alert-dialog';
import { useErrorNotification } from '@konfigyr/components/error';
import { useRemoveProfile } from '@konfigyr/hooks';
import React, { useCallback, useMemo } from 'react';
import { toast } from 'sonner';
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { Profile } from '@konfigyr/hooks/vault/types';

type Props = {
  namespace: Namespace,
  service: Service,
  profile?: Profile,
  onClose: () => void
};

export function DeleteConfigurationProfileAlert ({ namespace, service, profile, onClose }: Props) {
  const errorNotification = useErrorNotification();
  const open = useMemo(() => !!profile , [profile]);
  const { isPending: isPending, mutateAsync: removeProfile } = useRemoveProfile(namespace, service);

  const onOpenChange = useCallback((state: boolean) => {
    if (!state) {
      onClose();
    }
  }, [onClose]);

  const onRemove = useCallback(async () => {
    try {
      await removeProfile(profile!.slug);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<FormattedMessage
      defaultMessage="The {name} profile was successfully deleted."
      values={{
        name: <strong>{profile?.name}</strong>,
      }}
      description="Success message for deleting of a profile"
    />);
    return onClose();

  }, [profile, errorNotification]);

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Delete {name} profile"
              values={{
                name: profile?.name,
              }}
              description="Title of the modal that is shown when user tries to delete configuration profile"
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to delete {name} configuration profile?"
              values={{
                name: profile?.name,
              }}
              description="Confirmation text in the modal that is shown when user tries to delete a configuration profile"
            />
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction onClick={onRemove} disabled={isPending}>
            <YesLabel/>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}