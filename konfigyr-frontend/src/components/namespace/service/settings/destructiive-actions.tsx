import { Item, ItemActions, ItemContent, ItemDescription, ItemTitle } from '@konfigyr/components/ui/item';
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
  AlertDialogTrigger
} from '@konfigyr/components/ui/alert-dialog';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import { useErrorNotification } from '@konfigyr/components/error';
import { useRemoveNamespaceService } from '@konfigyr/hooks';
import { useCallback, useState } from 'react';
import { toast } from 'sonner';
import { CancelLabel, YesLabel } from '@konfigyr/components/messages';
import { Input } from '@base-ui/react/input';

export type ServiceSettingsProps = {
  namespace: Namespace,
  service: Service,
  onDelete: (service: Service) => void
}

export function ServiceDestructiveActions ({ namespace, service, onDelete }: ServiceSettingsProps) {
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <FormattedMessage
            defaultMessage="Destructive actions"
            description="Section in service settings for actions that cannot be undone"
          />
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-col gap-6">
          <DeleteServiceItem namespace={namespace} service={service} onDelete={onDelete}/>
        </div>
      </CardContent>
    </Card>
  );
}

export function DeleteServiceItem ({ namespace, service, onDelete }: ServiceSettingsProps) {

  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle>
          <FormattedMessage
            defaultMessage="Delete this service"
            description="Permanently removes this service and all its data"
          />

        </ItemTitle>
        <ItemDescription>
          <FormattedMessage
            defaultMessage="This action permanently deletes the service and cannot be undone."
            description="Short description of the service deletion action"
          />

        </ItemDescription>
      </ItemContent>

      <ItemActions>
        <ConfirmDeleteServiceAction namespace={namespace} service={service} onDelete={onDelete}/>
      </ItemActions>
    </Item>
  );
}

export function ConfirmDeleteServiceAction ({ namespace, service, onDelete }: ServiceSettingsProps) {
  const errorNotification = useErrorNotification();

  const intl = useIntl();

  const [name, setName] = useState<string>('');

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

    toast.success(<FormattedMessage
      defaultMessage="The {name}service was successfully deleted."
      values={{
        name: <strong>{service.name}</strong>,
      }}
      description="Success message for deleting of a service"
    />);

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

            <div className="flex justify-center">
              <Input
                placeholder={intl.formatMessage({
                  defaultMessage: 'Input service name',
                  description: 'Placeholder content for confirming service deletion',
                })}
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-2 h-11 w-100 text-center text-red-600 font-bold"
              />
            </div>

          </AlertDialogDescription>
        </AlertDialogHeader>
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
