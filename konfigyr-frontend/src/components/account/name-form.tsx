'use client';

import { z } from 'zod';
import { useCallback, useId} from 'react';
import { toast } from 'sonner';
import { IdCardIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useErrorNotification } from '@konfigyr/components/error';
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
import { useUpdateAccountName } from '@konfigyr/hooks';

import type { FormEvent } from 'react';
import type { Account } from '@konfigyr/hooks/types';

const nameFormSchema = z.object({
  name: z.string()
    .min(2, { message: 'Display name must be at least 2 characters.' })
    .max(32, { message: 'Display name must be at most 32 characters.' }),
});

export function AccountNameForm({ account }: { account: Account }) {
  const errorNotification = useErrorNotification();
  const { mutateAsync } = useUpdateAccountName();
  const id = useId();

  const form = useForm({
    defaultValues: { name: account.fullName },
    validators: {
      onChange: nameFormSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await mutateAsync(value.name);

        toast.success((
          <FormattedMessage
            defaultMessage="Your display name was updated"
            description="Notification message that is shown when user account display name was successfully updated"
          />
        ));
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
    <form.AppForm>
      <form name="account-name-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader>
            <CardTitle id={`label-name-${id}`} className="flex items-center gap-2">
              <CardIcon>
                <IdCardIcon size="1.25rem"/>
              </CardIcon>
              <FormattedMessage
                defaultMessage="Display name"
                description="Account display name form field label"
              />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-name-${id}`} className="mb-4">
              <FormattedMessage
                defaultMessage="This is how others will see you around the app. Keep it cool — or weird, we don’t judge."
                description="Account display name form field help text"
              />
            </p>

            <form.AppField name="name" children={(field) => (
              <field.Control>
                <field.Input
                  type="text"
                  aria-labelledby={`label-name-${id}`}
                  aria-describedby={`help-name-${id}`}
                />
              </field.Control>
            )} />

          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="Please use 32 characters at maximum."
                description="Account display name form field validation constraints"
              />
            </p>
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Update"
                  description="Account display name form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
