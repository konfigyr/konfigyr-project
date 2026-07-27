import { useCallback, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
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
import {
  Card,
  CardAction,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { Field } from '@konfigyr/components/ui/field';
import { Input } from '@konfigyr/components/ui/input';
import { toast } from '@konfigyr/components/ui/toast';
import { useErrorNotification } from '@konfigyr/components/error';
import { useRemoveNamespaceService } from '@konfigyr/hooks';
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

export type ServiceSettingsProps = {
  namespace: Namespace,
  service: Service,
  onDelete: (service: Service) => void
};

export function ServiceDestructiveActions ({ namespace, service, onDelete }: ServiceSettingsProps) {
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <FormattedMessage
            defaultMessage="Delete this service"
            description="Permanently removes this service and all its data"
          />
        </CardTitle>
        <CardDescription>
          <FormattedMessage
            defaultMessage="This action permanently deletes the service and cannot be undone."
            description="Short description of the service deletion action"
          />
        </CardDescription>
        <CardAction>
          <ConfirmDeleteServiceAction namespace={namespace} service={service} onDelete={onDelete}/>
        </CardAction>
      </CardHeader>
    </Card>
  );
}

export function ConfirmDeleteServiceAction ({ namespace, service, onDelete }: ServiceSettingsProps) {
  const intl = useIntl();
  const [name, setName] = useState<string>('');
  const errorNotification = useErrorNotification();

  const {
    isPending: isPending,
    mutateAsync: removeNamespaceService,
  } = useRemoveNamespaceService(namespace.slug);

  const onClickConfirm = useCallback(async () => {
    try {
      await removeNamespaceService(service.slug);
      onDelete(service);
    } catch (error) {
      return errorNotification(error);
    }

    toast.add({
      type: 'success',
      title: (
        <FormattedMessage
          defaultMessage="The {name}service was successfully deleted."
          description="Success message for deleting of a service"
          values={{ name: <strong>{service.name}</strong> }}
        />
      ),
    });

  }, [service, errorNotification]);

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant="destructive">
            <FormattedMessage
              defaultMessage="Delete service"
              description="Button label that triggers service delete confirmation dialog when clicked"
            />
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <FormattedMessage
              defaultMessage="Delete {name} service"
              values={{
                name: <strong>{service.name}</strong>,
              }}
              description="Title of the modal that is shown when user tries to delete service"
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to delete this service? This action cannot be undone. Please enter the service name to confirm deletion: "
              description="Confirmation text in the modal that is shown when user tries to delete a service"
            />
          </AlertDialogDescription>
        </AlertDialogHeader>

        <Field className="flex justify-center">
          <Input
            placeholder={intl.formatMessage({
              defaultMessage: 'Input service name',
              description: 'Placeholder content for confirming service deletion',
            })}
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="text-destructive"
          />
        </Field>

        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction onClick={onClickConfirm} disabled={isPending || name !== service.name}>
            <YesLabel/>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
