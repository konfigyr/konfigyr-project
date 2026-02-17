import { z } from 'zod';
import slugify from 'slugify';
import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { useCreateProfile } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  FieldDescription,
  FieldGroup,
  FieldLegend,
  FieldSeparator,
  FieldSet,
} from '@konfigyr/components/ui/field';
import { useForm } from '@konfigyr/components/ui/form';
import { PolicyPicker } from './policy-picker';
import {
  ProfileDescriptionHelpText,
  ProfileDescriptionLabel,
  ProfileNameHelpText,
  ProfileNameLabel,
  ProfilePositionHelpText,
  ProfilePositionLabel,
  ProfileSlugHelpText,
  ProfileSlugLabel,
} from './messages';

import type { FormEvent } from 'react';
import type { Namespace, Profile, Service } from '@konfigyr/hooks/types';

const AVAILABLE_POLICIES: Array<Profile['policy']> = ['PROTECTED', 'UNPROTECTED'] as const;

const profileFormSchema = z.object({
  name: z.string()
    .min(2, { message: 'Name must be at least 2 characters.' })
    .max(30, { message: 'Name must be at most 30 characters.' }),
  slug: z.string()
    .min(2, { message: 'URL slug must be at least 2 characters.' })
    .max(30, { message: 'URL slug must be at most 30 characters.' }),
  description: z.string()
    .max(255, { message: 'Description must be at most 255 characters.' }),
  policy: z.enum(['PROTECTED', 'UNPROTECTED']),
  position: z.number()
    .min(0, { message: 'Position must be at least 0.' }),
});

export function CreateProfileForm({ namespace, service, onCreate }: {
  namespace: Namespace,
  service: Service,
  onCreate: (profile: Profile) => void | Promise<void>,
}) {
  const { mutateAsync: createProfile } = useCreateProfile(namespace, service);
  const errorNotification = useErrorNotification();

  const form = useForm({
    defaultValues: {
      name: '',
      slug: '',
      description: '',
      policy: 'UNPROTECTED',
      position: 1,
    },
    validators: {
      onSubmit: profileFormSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        const profile = await createProfile(value);
        await onCreate(profile);
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
      <form onSubmit={onSubmit} className="grid gap-8">
        <FieldSet>
          <FieldGroup>
            <form.AppField
              name="name"
              listeners={{
                onChange: ({ value }) => generateSlug(value),
              }}
              children={(field) => (
                <field.Control
                  label={<ProfileNameLabel />}
                  description={<ProfileNameHelpText />}
                >
                  <field.Input type="text" />
                </field.Control>
              )}
            />

            <form.AppField
              name="slug"
              validators={{
                onChangeAsyncDebounceMs: 300,
              }}
              children={(field) => (
                <field.Control
                  label={<ProfileSlugLabel />}
                  description={<ProfileSlugHelpText />}
                >
                  <field.Input type="text" />
                </field.Control>
              )}
            />

            <form.AppField name="description" children={(field) => (
              <field.Control
                label={<ProfileDescriptionLabel />}
                description={<ProfileDescriptionHelpText />}
              >
                <field.Textarea rows={6} />
              </field.Control>
            )} />
          </FieldGroup>
        </FieldSet>

        <FieldSeparator />

        <FieldSet>
          <FieldLegend>
            <FormattedMessage
              defaultMessage="Change governance policy"
              description="Profile policy fieldset title"
            />
          </FieldLegend>
          <FieldDescription>
            <FormattedMessage
              defaultMessage="Defines how configuration changes are applied to this profile. The policy can be changed later, but doing so may affect how future changes are applied."
              description="Profile policy fieldset description"
            />
          </FieldDescription>
          <FieldGroup>
            <form.AppField name="policy" children={(field) => (
              <PolicyPicker
                options={AVAILABLE_POLICIES}
                value={field.getValue() as Profile['policy']}
                onChange={field.setValue}
              />
            )} />
          </FieldGroup>
        </FieldSet>

        <FieldSeparator />

        <FieldSet>
          <FieldLegend>
            <FormattedMessage
              defaultMessage="Display settings"
              description="Profile display settings fieldset title"
            />
          </FieldLegend>
          <FieldDescription>
            <FormattedMessage
              defaultMessage="Customize how this profile is going to be displayed in your service."
              description="Profile display settings fieldset description"
            />
          </FieldDescription>

          <FieldGroup>
            <form.AppField
              name="position"
              children={(field) => (
                <field.Control
                  label={<ProfilePositionLabel />}
                  description={<ProfilePositionHelpText />}
                >
                  <field.Input type="number" />
                </field.Control>
              )}
            />
          </FieldGroup>
        </FieldSet>

        <FieldSeparator />

        <div>
          <form.Submit>
            <FormattedMessage
              defaultMessage="Create profile"
              description="Namespace form submit button label"
            />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
