import { z } from 'zod';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useCreateKeyset } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { useForm } from '@konfigyr/components/ui/form';
import { KeysetAlgorithmSelect } from './keyset-algorithm';
import { CreateKeysetLabel } from './messages';

import type { FormEvent } from 'react';
import type { Keyset, Namespace } from '@konfigyr/hooks/types';

const keysetSchema = z.object({
  name: z.string()
    .nonempty({ message: 'Keyset name can not be blank' })
    .min(3, { message: 'Keyset name must be at least 3 characters long' })
    .max(30, { message: 'Keyset name must be at most 30 characters long' }),
  algorithm: z.string()
    .nonempty({ message: 'Keyset algorithm can not be blank' }),
  description: z.string()
    .max(255, { message: 'Keyset description must be at most 255 characters long' }),
  tags: z.array(z.string()),
});

export function CreateKeysetForm({ namespace, onCreate }: { namespace: Namespace, onCreate: (keyset: Keyset) => void }) {
  const errorNotification = useErrorNotification();
  const { mutateAsync: createKeyset } = useCreateKeyset(namespace.slug);

  const form = useForm({
    defaultValues: { algorithm: '', name: '', description: '', tags: [] } as z.infer<typeof keysetSchema>,
    validators: {
      onSubmit: keysetSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const keyset = await createKeyset(value);
        onCreate(keyset);
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
      <form name="create-keyset-form" className="grid gap-4" onSubmit={onSubmit}>
        <form.AppField name="name" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Keyset name"
              description="Label for the keyset name form field"
            />}
            description={<FormattedMessage
              defaultMessage="Enter a unique, recognizable name for this keyset, e.g., payment-service-prod."
              description="Help text for the keyset algorithm name field"
            />}
          >
            <field.Input />
          </field.Control>
        )} />

        <form.AppField name="algorithm" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Keyset algorithm"
              description="Label for the keyset algorithm form field"
            />}
            description={<FormattedMessage
              defaultMessage="Select the underlying cipher used for operations."
              description="Help text for the keyset algorithm form field"
            />}
          >
            <KeysetAlgorithmSelect
              className="w-full"
              value={field.state.value}
              detailed={true}
              placeholder={<FormattedMessage
                defaultMessage="Select algorithm..."
                description="Placeholder for the keyset algorithm form select field"
              />}
              onChange={field.handleChange}
            />
          </field.Control>
        )} />

        <form.AppField name="description" children={(field) => (
          <field.Control
            label={<FormattedMessage
              defaultMessage="Purpose"
              description="Label for the keyset name form field"
            />}
            description={<FormattedMessage
              defaultMessage="Briefly explain what data this keyset will protect. Use this to provide context for other administrators, you can leave it blank."
              description="Help text for the keyset algorithm description field"
            />}
          >
            <field.Textarea />
          </field.Control>
        )} />

        <form.Submit>
          <CreateKeysetLabel />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
