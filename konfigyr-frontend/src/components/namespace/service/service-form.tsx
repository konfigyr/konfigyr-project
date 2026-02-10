import { z } from 'zod';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useCreateNamespaceService } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { useForm } from '@konfigyr/components/ui/form';
import { Separator } from '@konfigyr/components/ui/separator';
import {
  ServiceDescriptionHelpText,
  ServiceDescriptionLabel,
  ServiceNameLabel,
} from './messages';

import type { FormEvent } from 'react';
import type { Namespace, Service } from '@konfigyr/hooks/types';

const serviceFormSchema = z.object({
  name: z.string()
    .min(5, { message: 'Display name must be at least 5 characters.' })
    .max(30, { message: 'Display name must be at most 30 characters.' }),
  slug: z.string()
    .min(5, { message: 'Identifier must be at least 5 characters.' })
    .max(30, { message: 'Identifier must be at most 30 characters.' }),
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
});

export function CreateServiceForm({ namespace, onCreate }: { namespace: Namespace, onCreate: (service: Service) => void | Promise<void> }) {
  const { mutateAsync: createService } = useCreateNamespaceService(namespace.slug);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      name: '',
      slug: '',
      description: '',
    },
    validators: {
      onSubmit: serviceFormSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const service = await createService(value);
        await onCreate(service);
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
      <form className="grid gap-6" onSubmit={onSubmit}>
        <form.AppField
          name="slug"
          validators={{
            onChangeAsyncDebounceMs: 300,
            // onChangeAsync: ({ value }) => validateSlug(queryClient, value),
          }}
          children={(field) => (
            <field.Control
              label={<FormattedMessage
                defaultMessage="Identifier"
                description="The form label used in the service forms to define the service URL slug input field"
              />}
              description={<FormattedMessage
                defaultMessage="This is your service’s unique identifier within this namespace. It must be identical to your application's configured name to ensure properties are loaded correctly."
                description="The help text used in the service forms to provide instructions how service URL should be defined"
              />}
            >
              <field.Input type="text"/>
            </field.Control>
          )}
        />

        <form.AppField
          name="name"
          children={(field) => (
            <field.Control
              label={<ServiceNameLabel />}
            >
              <field.Input type="text"/>
            </field.Control>
          )}
        />

        <form.AppField name="description" children={(field) => (
          <field.Control
            label={<ServiceDescriptionLabel />}
            description={<ServiceDescriptionHelpText />}
          >
            <field.Textarea rows={6} />
          </field.Control>
        )} />

        <Separator />

        <form.Submit>
          <FormattedMessage
            defaultMessage="Create service"
            description="Service form submit button label"
          />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
