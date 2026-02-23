import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useUpdateNamespaceService } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  Card,
  CardAction,
  CardContent,
  CardFooter,
  CardHeader,
} from '@konfigyr/components/ui/card';
import { useForm } from '@konfigyr/components/ui/form';
import {
  ServiceDescriptionHelpText,
  ServiceDescriptionLabel,
  ServiceNameLabel,
} from './messages';

import type { FormEvent } from 'react';
import type { Namespace, Service } from '@konfigyr/hooks/types';

const serviceUpdateSchema = z.object({
  slug: z.string(),
  name: z.string()
    .min(3, { message: 'Name must be at least 3 characters.' })
    .max(30, { message: 'Name must be at most 30 characters.' }),
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
});

export function ServiceUpdateForm({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { mutateAsync: updateService } = useUpdateNamespaceService(namespace.slug, service.slug);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      slug: service.slug,
      name: service.name || '',
      description: service.description || '',
    },
    validators: {
      onChange: serviceUpdateSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await updateService(value);
      } catch (error) {
        return errorNotification(error);
      }

      return toast.success((
        <FormattedMessage
          defaultMessage="Your service was updated"
          description="Notification message that is shown when service was successfully updated"
        />
      ));
    },
  });

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  return (
    <form.AppForm>
      <form name="namespace-name-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader title="General settings" />
          <CardContent className="grid gap-6">
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
          </CardContent>
          <CardFooter className="justify-between border-t">
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Update service"
                  description="Service update form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
