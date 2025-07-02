'use client';

import type { Account } from 'konfigyr/services/authentication';
import type { FieldValues } from 'react-hook-form';

import { toast } from 'sonner';
import { useId } from 'react';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import request, { HttpResponseError, type ProblemDetail } from 'konfigyr/services/http';
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardFooter,
  CardAction,
  Input,
  Form,
  FormField,
  FormControl,
  useForm,
} from 'konfigyr/components/ui';
import { useAccount } from './context';

const nameFormSchema = z.object({
  name: z.string()
    .min(2, { message: 'Display name must be at least 2 characters.' })
    .max(32, { message: 'Display name must be at most 32 characters.' }),
});

const updateAccount = async (data: FieldValues): Promise<{ account?: Account, error?: ProblemDetail }> => {
  try {
    const account = await request<Account>('/api/account', {
      method: 'PATCH',
      body: JSON.stringify(data),
      headers: { 'content-type': 'application/json' },
    });

    return { account };
  } catch (error) {
    if (error instanceof HttpResponseError) {
      return { error: error.problem };
    }

    if (error instanceof Error) {
      return { error: { detail: error.message } };
    }

    return { error: { detail: 'Unexpected error occurred' } };
  }
};

export function AccountNameForm() {
  const id = useId();
  const [account, setAccount] = useAccount();
  const t = useTranslations('account.form.name');

  const form = useForm<z.infer<typeof nameFormSchema>>({
    resolver: zodResolver(nameFormSchema),
    defaultValues: { name: account?.fullName },
  });

  const handler = async (data: FieldValues) => {
    const { account, error } = await updateAccount(data);

    if (error) {
      toast.error(error.title, { description: error.detail });
    } else if (account) {
      setAccount(account);
      toast(t('success'));
    }
  };

  return (
    <Form {...form}>
      <form name="account-name-form" onSubmit={form.handleSubmit(handler)}>
        <Card className="my-8">
          <CardHeader>
            <CardTitle id={`label-name-${id}`}>
              {t('label')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-name-${id}`} className="mb-4">
              {t('help')}
            </p>
            <FormControl
              name="name"
              render={({field}) => (
                <FormField>
                  <Input
                    type="text"
                    aria-labelledby={`label-name-${id}`}
                    aria-describedby={`help-name-${id}`}
                    {...field}
                  />
                </FormField>
              )}
            />
          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">{t('validation')}</p>
            <CardAction>
              <Button
                type="submit"
                disabled={form.formState.disabled || !form.formState.isValid}
                loading={form.formState.isSubmitting || form.formState.isValidating}
              >
                {t('submit')}
              </Button>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </Form>
  );
}
