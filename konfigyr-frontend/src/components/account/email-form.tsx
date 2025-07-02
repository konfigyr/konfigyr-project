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

async function executeRequest<T>(options: RequestInit): Promise<{ result?: T, error?: ProblemDetail }> {
  try {
    const result = await request<T>('/api/account/email', {
      headers: { 'content-type': 'application/json' },
      ...options,
    });

    return { result };
  } catch (error) {
    if (error instanceof HttpResponseError) {
      return { error: error.problem };
    }

    if (error instanceof Error) {
      return { error: { detail: error.message } };
    }

    return { error: { detail: 'Unexpected error occurred' } };
  }
}

export function AccountEmailForm() {
  const id = useId();
  const [account, setAccount] = useAccount();
  const t = useTranslations('account.form.email');

  const form = useForm<z.infer<typeof emailFormSchema>>({
    resolver: zodResolver(emailFormSchema),
    defaultValues: { email: account?.email, token: '', code: '' },
  });

  const isTokenPresent = Boolean(form.getValues('token'));

  const handler = async (data: FieldValues) => {
    let problem: ProblemDetail | undefined;

    if (isTokenPresent) {
      const { result, error } = await executeRequest<Account>({
        method: 'PUT', body: JSON.stringify({ token: data.token, code: data.code }),
      });

      if (result) {
        toast(t('success'));
        setAccount(result);
        form.reset();
        form.setValue('email', result.email);
      } else {
        problem = error;
      }
    } else {
      const { result, error } = await executeRequest<{ token: string }>({
        method: 'POST', body: JSON.stringify({ email: data.email }),
      });

      if (result?.token) {
        form.setValue('token', result.token, { shouldValidate: true });
      } else {
        problem = error;
      }
    }

    if (problem) {
      toast.error(problem.title, { description: problem.detail });
    }
  };

  return (
    <Form {...form}>
      <form name="account-email-form" onSubmit={form.handleSubmit(handler)}>
        <Card className="my-8">
          <CardHeader>
            <CardTitle id={`label-email-${id}`}>
              {t('label')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-email-${id}`} className="mb-4">
              {t('help')}
            </p>
            <FormControl
              name="email"
              render={({field}) => (
                <FormField>
                  <Input
                    type="text"
                    aria-labelledby={`label-email-${id}`}
                    aria-describedby={`help-email-${id}`}
                    disabled={isTokenPresent}
                    {...field}
                  />
                </FormField>
              )}
            />

            <FormControl
              name="code"
              render={({field}) => (
                <>
                  {isTokenPresent && (
                    <FormField
                      className="mt-4"
                      label={t('confirmation.label')}
                      description={t('confirmation.description')}
                    >
                      <Input type="text" {...field} />
                    </FormField>
                  )}
                </>
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
