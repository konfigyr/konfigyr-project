import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback, useId } from 'react';
import { IdCardIcon } from 'lucide-react';
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
  NamespaceNameDescription,
  NamespaceNameLabel,
} from './messages';

import type { FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

const namespaceNameSchema = z.object({
  name: z.string()
    .min(3, { message: 'Name must be at least 3 characters.' })
    .max(30, { message: 'Name must be at most 30 characters.' }),
});

export function NamespaceNameForm({ namespace }: { namespace: Namespace }) {
  const id = useId();
  const { mutateAsync: updateNamespace } = useUpdateNamespace(namespace);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      name: namespace.name || '',
    },
    validators: {
      onChange: namespaceNameSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await updateNamespace(value);
      } catch (error) {
        return errorNotification(error);
      }

      return toast.success((
        <FormattedMessage
          defaultMessage="Your namespace name was updated"
          description="Notification message that is shown when namespace name was successfully updated"
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
          <CardHeader>
            <CardTitle id={`label-name-${id}`} className="flex items-center gap-2">
              <CardIcon>
                <IdCardIcon size="1.25rem"/>
              </CardIcon>
              <NamespaceNameLabel />
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p id={`help-name-${id}`} className="mb-4">
              <NamespaceNameDescription />
            </p>

            <form.AppField name="name" children={(field) => (
              <field.Control>
                <field.Input aria-labelledby={`label-name-${id}`} aria-describedby={`help-name-${id}`} />
              </field.Control>
            )} />

          </CardContent>
          <CardFooter className="justify-between border-t">
            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="Please use 30 characters at maximum."
                description="Namespace name form field validation constraints"
              />
            </p>
            <CardAction>
              <form.Submit>
                <FormattedMessage
                  defaultMessage="Update display name"
                  description="Namespace name form submit button label"
                />
              </form.Submit>
            </CardAction>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
