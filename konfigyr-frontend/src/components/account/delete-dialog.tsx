'use client';

import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { UserXIcon } from 'lucide-react';
import { useErrorNotification } from '@konfigyr/components/error';
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
import type { Account } from '@konfigyr/hooks/types';

export function AccountDeleteConfirmationDialog({ account, onDelete }: { account: Account, onDelete: () => Promise<void> | void }) {
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: { account: account.id },
    onSubmit: async () => {
      try {
        await onDelete();
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
                defaultMessage="Delete account"
                description="Label for account delete form card"
              />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="mb-4">
              <FormattedMessage
                defaultMessage="This will nuke your account and all your data, there is no going back. Only do this if you're 100% sure. Seriously."
                description="Help text that explains the consequences of deleting an account"
              />
            </p>
          </CardContent>
          <CardFooter className="justify-end border-t">
            <CardAction>
              <AlertDialogTrigger asChild>
                <Button variant="destructive">
                  <FormattedMessage
                    defaultMessage="Delete account"
                    description="Button label that triggers account delete confirmation dialog when clicked"
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
                description="Title of account delete confirmation dialog"
              />
            </AlertDialogTitle>
          </AlertDialogHeader>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="This action cannot be undone. This will permanently delete your account and remove your data from our servers."
              description="Confirmation text that explains the consequences of deleting an account"
            />
          </AlertDialogDescription>

          <AlertDialogFooter>
            <AlertDialogCancel>
              <FormattedMessage
                defaultMessage="Nope, I have changed my mind"
                description="Button label that closes account delete confirmation dialog when clicked"
              />
            </AlertDialogCancel>
            <form.AppForm>
              <form onSubmit={onSubmit}>
                <form.Submit>
                  <FormattedMessage
                    defaultMessage="Yes, I am sure"
                    description="Button label that deletes account when clicked"
                  />
                </form.Submit>
              </form>
            </form.AppForm>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
