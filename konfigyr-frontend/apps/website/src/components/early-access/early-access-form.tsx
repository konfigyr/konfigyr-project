'use client';

import { useState } from 'react';
import { z } from 'zod';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { Card, CardContent } from '@konfigyr/ui/components/card';
import { Checkbox } from '@konfigyr/ui/components/checkbox';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@konfigyr/ui/components/field';
import { useForm, useFormSubmit } from '@konfigyr/ui/components/form';
import { TEAM_SIZE_OPTIONS, TeamSizeSelect } from '@konfigyr/components/early-access/team-size-select';

import type { ChangeEvent } from 'react';
import type { EarlyAccessSubmission, SubmissionResult } from '@konfigyr/routes/early-access/-handler';

const messages = defineMessages({
  name: {
    defaultMessage: 'Name',
    description: 'Label for the name field on the early access form',
  },
  email: {
    defaultMessage: 'Work email',
    description: 'Label for the work email field on the early access form',
  },
  company: {
    defaultMessage: 'Company',
    description: 'Label for the company field on the early access form',
  },
  teamSize: {
    defaultMessage: 'Team / org size',
    description: 'Label for the team size field on the early access form',
  },
  teamSizePlaceholder: {
    defaultMessage: 'Select team size',
    description: 'Placeholder for the team size field on the early access form',
  },
  serviceCount: {
    defaultMessage: 'How many Spring Boot services are you running (roughly)?',
    description: 'Label for the Spring Boot service count field on the early access form',
  },
  currentTooling: {
    defaultMessage: 'What are you using today for config or secrets, if anything?',
    description: 'Label for the current tooling field on the early access form',
  },
  currentToolingHint: {
    defaultMessage: 'e.g. HashiCorp Vault, Infisical, Doppler, application.yml / manual, a cloud provider\'s config or secrets service, nothing formal',
    description: 'Help text for the current tooling field on the early access form',
  },
  notes: {
    defaultMessage: 'Anything else you want us to know?',
    description: 'Label for the additional notes field on the early access form',
  },
  consent: {
    defaultMessage: 'I agree to be contacted about early access, per the <link>Privacy Policy</link>.',
    description: 'Label for the consent checkbox on the early access form, contains a link to the privacy policy',
  },
  submit: {
    defaultMessage: 'Request Early Access',
    description: 'Submit button label on the early access form',
  },
  submitFailed: {
    defaultMessage: 'Something went wrong submitting your request.',
    description: 'Error message shown when the early access form submission fails',
  },
  confirmation: {
    defaultMessage: 'We read every request ourselves. If it looks like a good fit, we\'ll follow up to set up a short call. No automated drip sequence, no sales deck.',
    description: 'Confirmation message shown after the early access form is successfully submitted',
  },
});

const earlyAccessSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.email('Enter a valid work email'),
  company: z.string().min(1, 'Company is required'),
  teamSize: z.enum(TEAM_SIZE_OPTIONS, { message: 'Select your team size' }),
  serviceCount: z.string().min(1, 'Please provide an estimate'),
  currentTooling: z.string(),
  notes: z.string(),
  consent: z.literal(true, { message: 'Please confirm to continue' }),
  hp_field: z.string(),
});

function normalizeFieldError(e: unknown): { message?: string } {
  if (typeof e === 'string') return { message: e };
  if (e && typeof e === 'object' && 'message' in e) return e as { message?: string };
  return {};
}

export interface EarlyAccessFormProps {
  onSubmit: (submission: EarlyAccessSubmission) => Promise<SubmissionResult>;
}

export function EarlyAccessForm({ onSubmit }: EarlyAccessFormProps) {
  const intl = useIntl();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  const form = useForm({
    defaultValues: {
      name: '',
      email: '',
      company: '',
      teamSize: '' as (typeof TEAM_SIZE_OPTIONS)[number] | '',
      serviceCount: '',
      currentTooling: '',
      notes: '',
      consent: false,
      hp_field: '',
    },
    validators: {
      onSubmit: earlyAccessSchema,
    },
    onSubmit: async ({ value }) => {
      setSubmitError(null);

      const result = await onSubmit(value);

      if (result.ok) {
        setIsSuccess(true);
      } else {
        setSubmitError(result.detail ?? intl.formatMessage(messages.submitFailed));
      }
    },
  });

  const handleSubmit = useFormSubmit(form);

  if (isSuccess) {
    return (
      <Card>
        <CardContent>
          <p className="text-center leading-relaxed">
            <FormattedMessage {...messages.confirmation} />
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <form.AppForm>
      <form name="early-access-form" className="grid gap-6" onSubmit={handleSubmit}>
        <Card>
          <CardContent className="grid gap-6">
            <form.AppField name="name" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.name} />}
                render={<field.Input type="text" autoComplete="name" />}
              />
            )} />

            <form.AppField name="email" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.email} />}
                render={<field.Input type="email" autoComplete="email" />}
              />
            )} />

            <form.AppField name="company" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.company} />}
                render={<field.Input type="text" autoComplete="organization" />}
              />
            )} />

            <form.AppField name="teamSize" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.teamSize} />}
                render={
                  <TeamSizeSelect
                    className="w-full"
                    value={field.state.value}
                    placeholder={<FormattedMessage {...messages.teamSizePlaceholder} />}
                    onChange={field.handleChange}
                  />
                }
              />
            )} />

            <form.AppField name="serviceCount" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.serviceCount} />}
                render={<field.Input type="text" />}
              />
            )} />

            <form.AppField name="currentTooling" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.currentTooling} />}
                description={<FormattedMessage {...messages.currentToolingHint} />}
                render={<field.Input type="text" />}
              />
            )} />

            <form.AppField name="notes" children={field => (
              <field.Control
                label={<FormattedMessage {...messages.notes} />}
                render={<field.Textarea />}
              />
            )} />

            <form.AppField name="consent" children={(field) => (
              <Field orientation="horizontal" data-invalid={!field.state.meta.isValid}>
                <Checkbox
                  id="consent"
                  name="consent"
                  checked={field.state.value}
                  aria-invalid={!field.state.meta.isValid}
                  onCheckedChange={checked => field.handleChange(checked === true)}
                  onBlur={() => field.handleBlur()}
                />
                <div className="grid gap-1">
                  <FieldLabel htmlFor="consent">
                    <FormattedMessage
                      {...messages.consent}
                      values={{
                        link: (chunks) => (
                          <a href="/privacy" key="link" className="underline underline-offset-4">
                            {chunks}
                          </a>
                        ),
                      }}
                    />
                  </FieldLabel>
                  <FieldError errors={field.state.meta.errors.map(normalizeFieldError)} />
                </div>
              </Field>
            )} />

            {/* Honeypot: real visitors never see or fill this field. Checked only server-side. */}
            <form.Field name="hp_field">
              {(field: { state: { value: string }; handleChange: (value: string) => void }) => (
                <input
                  type="text"
                  name="hp_field"
                  value={field.state.value}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => field.handleChange(e.target.value)}
                  autoComplete="off"
                  tabIndex={-1}
                  aria-hidden="true"
                  className="absolute left-[-9999px] top-auto w-px h-px overflow-hidden"
                />
              )}
            </form.Field>

            {submitError && (
              <FieldDescription className="text-destructive">
                {submitError}
              </FieldDescription>
            )}
          </CardContent>
        </Card>

        <div>
          <form.Submit>
            <FormattedMessage {...messages.submit} />
          </form.Submit>
        </div>
      </form>
    </form.AppForm>
  );
}
