import { z } from 'zod';
import { format } from 'date-fns';
import { useCallback, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { useErrorNotification } from '@konfigyr/components/error';
import { useForm } from '@konfigyr/components/ui/form';
import {
  CreateNamespaceApplicationLabel,
  UpdateNamespaceApplicationLabel,
} from '@konfigyr/components/namespace/applications/messages';
import { Checkbox} from '@konfigyr/components/ui/checkbox';
import { Label } from '@konfigyr/components/ui/label';
import type { FormEvent } from 'react';
import type { Namespace, NamespaceApplication} from '@konfigyr/hooks/types';

const SCOPES : Array<string> = ['namespaces:read', 'namespaces:write', 'namespaces:delete', 'namespaces:invite', 'namespaces'];

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
  scopes:  z
    .array(z.string())
    .refine(
      (values) => values.every((v) => SCOPES.includes(v)),
      'Invalid scope selected',
    ),
});

export function NamespaceApplicationForm({ namespace, namespaceApplication, handleSubmit }: { namespace: Namespace, namespaceApplication?: Partial<NamespaceApplication>, handleSubmit: (namespaceApplication: z.infer<typeof namespaceApplicationSchema>) => void }) {
  const errorNotification = useErrorNotification();
  const defaultValues: z.infer<typeof namespaceApplicationSchema> = useMemo(() => ({
    name: namespaceApplication?.name ?? '',
    scopes: namespaceApplication?.scopes?.split(' ') ?? [],
    expiresAt: namespaceApplication?.expiresAt ? format(new Date(namespaceApplication.expiresAt), 'yyyy-MM-dd') : '',
  }), [namespaceApplication]);
  const form = useForm({
    defaultValues: defaultValues,
    validators: {
      onSubmit: namespaceApplicationSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await handleSubmit(value);
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
          >
            <field.Input />
          </field.Control>
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
          >
            <field.Input type="date" />
          </field.Control>
        )} />

        <form.AppField name="scopes" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Select scopes"
              description="Label for the application scopes field"
            />}
            description={<FormattedMessage
              defaultMessage="List of scopes that the application has access to"
              description="Help text for the namespace application scopes field"
            />}
          >
            <div className="space-y-2">
              {SCOPES.map((scope) => {
                const checked = field.state.value.includes(scope);
                return (
                  <div key={scope} className="flex items-center gap-2">
                    <Checkbox
                      key={scope}
                      id={scope}
                      checked={checked}
                      onCheckedChange={(isChecked) => {
                        const scopes = isChecked
                          ? [...field.state.value, scope]
                          : field.state.value.filter((s) => s !== scope);
                        field.handleChange(scopes);
                      }}
                    />
                    <Label htmlFor={scope}>{scope}</Label>
                  </div>
                );
              })}
            </div>
          </field.Control>
        )} />

        <form.Submit>
          { namespaceApplication?.id ? <UpdateNamespaceApplicationLabel /> : <CreateNamespaceApplicationLabel /> }
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
