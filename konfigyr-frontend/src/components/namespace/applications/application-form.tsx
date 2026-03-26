import { z } from 'zod';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { useErrorNotification } from '@konfigyr/components/error';
import { useForm } from '@konfigyr/components/ui/form';
import {
  CreateNamespaceApplicationLabel,
  UpdateNamespaceApplicationLabel,
} from '@konfigyr/components/namespace/applications/messages';
import { ApplicationScopesField } from './application-scopes-field';

import type { FormEvent } from 'react';
import type { CreateNamespaceApplication, Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

export const namespaceApplicationSchema = z.object({
  name: z.string()
    .nonempty({ message: 'Application name can not be blank' })
    .min(3, { message: 'Application name must be at least 3 characters long' })
    .max(30, { message: 'Application name must be at most 30 characters long' }),
  expiresAt: z.string()
    .optional()
    .refine((val) => !val || !isNaN(Date.parse(val)), {
      message: 'Expiration date must be a valid date',
    }),
  scopes: z.array(z.string()),
});

const extractApplicationScopes = (scopes?: string): Array<string> => {
  if (typeof scopes === 'string') {
    return scopes.split(' ').filter(scope => scope !== '');
  }
  return [];
};

const extractApplicationExpiryDate = (value?: string | number | Date): string => {
  let date;

  if (typeof value === 'string') {
    date = new Date(value);
  }
  if (typeof value === 'number') {
    date = new Date(value);
  }
  if (value instanceof Date) {
    date = value;
  }

  if (date) {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  return '';
};

export function NamespaceApplicationForm({ namespace, namespaceApplication, handleSubmit }: {
  namespace: Namespace,
  namespaceApplication?: NamespaceApplication,
  handleSubmit: (namespaceApplication: CreateNamespaceApplication) => void | Promise<void>
}) {
  const errorNotification = useErrorNotification();

  const defaultValues: z.infer<typeof namespaceApplicationSchema> = useMemo(() => ({
    name: namespaceApplication?.name ?? '',
    scopes: extractApplicationScopes(namespaceApplication?.scopes),
    expiresAt: extractApplicationExpiryDate(namespaceApplication?.expiresAt),
  }), [namespaceApplication]);

  const form = useForm({
    defaultValues: defaultValues,
    validators: {
      onSubmit: namespaceApplicationSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await handleSubmit({
          name: value.name,
          scopes: value.scopes.join(' '),
          expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
        });
      } catch (error) {
        return errorNotification(error);
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
      <form name="create-namespace-application-form" className="grid gap-4" onSubmit={onSubmit}>
        <form.AppField name="name" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Application name"
              description="Label for the application name form field"
            />}
            description={<FormattedMessage
              defaultMessage="Enter a name for the application."
              description="Help text for the namespace application name field"
            />}
            render={<field.Input />}
          />
        )} />

        <form.AppField name="expiresAt" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Expiration date"
              description="Label for the application expiration date field"
            />}
            description={<FormattedMessage
              defaultMessage="Select the date when this application should expire."
              description="Help text for the namespace application expiration date field"
            />}
            render={<field.Input type="date" />}
          />
        )} />

        <form.AppField name="scopes" children={(field) => (
          <field.Control
            render={
              <ApplicationScopesField
                namespace={namespace}
                value={field.state.value}
                onChange={field.handleChange}
              />
            }
          />
        )} />

        <form.Submit>
          { namespaceApplication?.id ? <UpdateNamespaceApplicationLabel /> : <CreateNamespaceApplicationLabel /> }
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
