'use client';

import { z } from 'zod';
import { useCallback, useId } from 'react';
import { toast } from 'sonner';
import { AtSignIcon } from 'lucide-react';
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
import {
  useConfirmAccountEmailChange,
  useUpdateAccountEmail,
} from '@konfigyr/hooks';

import type { FormEvent } from 'react';
import type { Account } from '@konfigyr/hooks/types';

const emailFormSchema = z.object({
  email: z.string()
    .email('You need to enter a valid email address')
    .max(48, { message: 'Email must be at most 48 characters.' }),
  token: z.string(),
  code: z.string(),
}).refine(schema => schema.token === '' || (schema.token && schema.code), {
  message: 'You need to enter a confirmation code',
  path: ['code'],
});

export function AccountEmailForm({ account }: { account: Account }) {
  const { mutateAsync: updateAccountMail } = useUpdateAccountEmail();
  const { mutateAsync: verifyCode } = useConfirmAccountEmailChange();
  const errorNotification = useErrorNotification();

  const id = useId();

  const form = useForm({
    defaultValues: { email: account.email, token: '', code: '' },
    validators: {
      onChange: emailFormSchema,
    },
    onSubmit: async ({ value }) => {
      try {

        if (isTokenPresent) {
          await verifyCode(value);
          form.reset();
        } else {
          const { token } = await updateAccountMail(value.email);
          return form.setFieldValue('token', token);
        }

        toast.success((
          <FormattedMessage
            defaultMessage="Your email address was updated"
            description="Notification message that is shown when user email address was successfully updated"
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

  const isTokenPresent = Boolean(form.getFieldValue('token'));

  return (
    <form.AppForm>
      <form name="account-email-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader>
            <CardTitle id={`label-email-${id}`} className="flex items-center gap-2">
              <CardIcon>
                <AtSignIcon size="1.25rem"/>
              </CardIcon>
              <FormattedMessage
                defaultMessage="Email address"
                description="Accont email address input field label"
              />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-email-${id}`} className="mb-4">
              <FormattedMessage
                defaultMessage="We will use this to send you important stuff — like password resets, not cat memes. Promise."
                description="Accont email address input field help text"
              />
            </p>

            <form.AppField name="email" children={(field) => (
              <field.Control>
                <field.Input
                  type="text"
                  aria-labelledby={`label-email-${id}`}
                  aria-describedby={`help-email-${id}`}
                />
              </field.Control>
            )} />

            {isTokenPresent && (
              <form.AppField name="code" children={(field) => (
                <field.Control
                  className="mt-4"
                  label={(
                    <FormattedMessage
                      defaultMessage="Enter confirmation code"
                      description="Label used for account email confirmation code input field"
                    />
                  )}
                  description={(
                    <FormattedMessage
                      defaultMessage="We have sent you a confirmation code to your new email address, please paste it here to confirm your email address change."
                      description="Help text describing that the email confirmation code email has been sent and it should be entered"
                    />
                  )}
                >
                  <field.Input type="text" />
                </field.Control>
              )} />
            )}

          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="Please use 48 characters at maximum."
                description="Accont email address input field validation constraints"
              />
            </p>
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Update"
                  description="Accont email address form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
