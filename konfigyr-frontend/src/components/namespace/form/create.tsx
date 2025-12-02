import { z } from 'zod';
import slugify from 'slugify';
import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { FormattedMessage } from 'react-intl';
import { useCreateNamespace } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import { useForm } from '@konfigyr/components/ui/form';
import { Separator } from '@konfigyr/components/ui/separator';
import { SlugDescription, validateSlug } from './slug';

import type { FormEvent } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

const namespaceFormSchema = z.object({
  name: z.string()
    .min(3, { message: 'Name must be at least 3 characters.' })
    .max(30, { message: 'Name must be at most 30 characters.' }),
  slug: z.string()
    .min(5, { message: 'URL slug must be at least 5 characters.' })
    .max(30, { message: 'URL slug must be at most 30 characters.' }),
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
});

export function CreateNamespaceForm({ onCreate }: { onCreate: (namespace: Namespace) => void | Promise<void> }) {
  const queryClient = useQueryClient();
  const { mutateAsync: createNamespace } = useCreateNamespace();
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      name: '',
      slug: '',
      description: '',
    },
    validators: {
      onSubmit: namespaceFormSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const namespace = await createNamespace(value);
        await onCreate(namespace);
      } catch (error) {
        errorNotification(error);
      }
    },
  });

  const generateSlug = (name: string) => {
    const state = form.getFieldMeta('slug');

    if (!state?.isPristine) {
      return;
    }

    form.setFieldValue('slug', slugify(name, { lower: true }), {
      dontUpdateMeta: true,
      dontRunListeners: true,
    });
  };

  const onSubmit = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    event.stopPropagation();

    return form.handleSubmit(event);
  }, [form.handleSubmit]);

  return (
    <form.AppForm>
      <form className="grid gap-6" onSubmit={onSubmit}>
        <form.AppField
          name="name"
          listeners={{
            onChange: ({ value }) => generateSlug(value),
          }}
          children={(field) => (
            <field.Control
              label={<FormattedMessage
                id="namespaces.form.name.label"
                defaultMessage="Name"
              />}
              description={<FormattedMessage
                id="namespaces.form.name.description"
                defaultMessage="The official name of your namespace. This is how your team, project, or organization will be identified. Keep it clear and recognizable!"
              />}
            >
              <field.Input type="text" />
            </field.Control>
          )}
        />

        <form.AppField
          name="slug"
          validators={{
            onChangeAsyncDebounceMs: 300,
            onChangeAsync: ({ value }) => validateSlug(queryClient, value),
          }}
          children={(field) => (
            <field.Control
              label={<FormattedMessage
                id="namespaces.form.slug.label"
                defaultMessage="URL"
              />}
              description={<SlugDescription />}
            >
              <field.Input type="text" />
            </field.Control>
          )}
        />

        <form.AppField name="description" children={(field) => (
          <field.Control
            label={<FormattedMessage
              id="namespaces.form.description.label"
              defaultMessage="Description"
            />}
            description={<FormattedMessage
              id="namespaces.form.description.description"
              defaultMessage="Tell the world (or just your team) what this namespace is all about. A short and sweet explanation of its purpose, goals, or mission. Or not..."
            />}
          >
            <field.Textarea rows={6} />
          </field.Control>
        )} />

        <Separator />

        <form.Submit>
          <FormattedMessage
            id="namespaces.form.submit.create"
            defaultMessage="Create namespace"
          />
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
