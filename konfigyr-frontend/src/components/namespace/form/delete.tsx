'use client';

import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { UserXIcon } from 'lucide-react';
import { useErrorNotification } from '@konfigyr/components/error';
import { CancelLabel, DeleteLabel } from '@konfigyr/components/messages';
import {
  AlertDialog,
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
  CardContent,
  CardFooter,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { useForm } from '@konfigyr/components/ui/form';

import type { FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export interface NamespaceDeleteFormProps {
  namespace: Namespace;
  onDelete: (namespace: Namespace) => Promise<void> | void;
}

export function NamespaceDeleteForm({ namespace, onDelete }: NamespaceDeleteFormProps) {
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: { namespace: namespace.id },
    onSubmit: async () => {
      try {
        await onDelete(namespace);
      } catch (error) {
        errorNotification(error);
      }
    },
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  return (
    <>
      <AlertDialog>
        <Card className="border">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CardIcon>
                <UserXIcon size="1.25rem"/>
              </CardIcon>
              <FormattedMessage
                defaultMessage="Delete namespace"
                description="Label for namespace delete form card"
              />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="mb-4">
              <FormattedMessage
                defaultMessage="Permanently remove this namespace and all its associated services, configurations, and metadata. This action is irreversible."
                description="Help text that explains the consequences of deleting a namespace"
              />
            </p>
          </CardContent>
          <CardFooter className="justify-end border-t">
            <CardAction>
              <AlertDialogTrigger asChild>
                <Button variant="destructive">
                  <FormattedMessage
                    defaultMessage="Delete namespace"
                    description="Button label that triggers namespace delete confirmation dialog when clicked"
                  />
                </Button>
              </AlertDialogTrigger>
            </CardAction>
          </CardFooter>
        </Card>

        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              <FormattedMessage
                defaultMessage="Are you sure?"
                description="Title of namespace delete confirmation dialog"
              />
            </AlertDialogTitle>
          </AlertDialogHeader>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="This action cannot be undone. This will permanently delete the namespace and all its associated data from our servers."
              description="Confirmation text that explains the consequences of deleting an namespace"
            />
          </AlertDialogDescription>

          <AlertDialogFooter>
            <AlertDialogCancel>
              <CancelLabel />
            </AlertDialogCancel>
            <form.AppForm>
              <form onSubmit={onSubmit}>
                <form.Submit>
                  <DeleteLabel />
                </form.Submit>
              </form>
            </form.AppForm>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
