import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback, useId } from 'react';
import { TextSelectIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { useUpdateNamespace } from '@konfigyr/hooks';
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
  NamespaceDescriptionHelpText,
  NamespaceDescriptionLabel,
} from './messages';

import type { FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

const namespaceDescriptionSchema = z.object({
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
});

export function NamespaceDescriptionForm({ namespace }: { namespace: Namespace }) {
  const id = useId();
  const { mutateAsync: updateNamespace } = useUpdateNamespace(namespace);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      description: namespace.description || '',
    },
    validators: {
      onChange: namespaceDescriptionSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await updateNamespace(value);
      } catch (error) {
        return errorNotification(error);
      }

      return toast.success((
        <FormattedMessage
          defaultMessage="Your namespace description was updated"
          description="Notification message that is shown when namespace description was successfully updated"
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
      <form name="namespace-description-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader>
            <CardTitle id={`label-description-${id}`} className="flex items-center gap-2">
              <CardIcon>
                <TextSelectIcon size="1.25rem"/>
              </CardIcon>
              <NamespaceDescriptionLabel />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-description-${id}`} className="mb-4">
              <NamespaceDescriptionHelpText />
            </p>

            <form.AppField name="description" children={(field) => (
              <field.Control>
                <field.Textarea
                  rows={8}
                  aria-labelledby={`label-description-${id}`}
                  aria-describedby={`help-description-${id}`}
                />
              </field.Control>
            )} />

          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="Please use 255 characters at maximum."
                description="Namespace description form field validation constraints"
              />
            </p>
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Update description"
                  description="Namespace description form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
