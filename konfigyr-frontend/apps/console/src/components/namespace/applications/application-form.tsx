import { z } from 'zod';
import { useMemo } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { createFormHook, createFormHookContexts } from '@tanstack/react-form';
import { useErrorNotification } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLegend,
  FieldSet,
} from '@konfigyr/components/ui/field';
import { Input } from '@konfigyr/components/ui/input';
import { Label } from '@konfigyr/components/ui/label';
import {
  FormControl,
  FormInput,
  FormSearchInput,
  FormSwitch,
  FormTextarea,
  SubmitButton,
  useFormSubmit,
} from '@konfigyr/components/ui/form';
import {
  CreateNamespaceApplicationLabel,
  UpdateNamespaceApplicationLabel,
} from '@konfigyr/components/namespace/applications/messages';
import { ApplicationScopesField } from './application-scopes-field';

import type {
  AgentApplicationSettings,
  CreateNamespaceApplication,
  Namespace,
  NamespaceApplication,
  NamespaceApplicationType,
  WorkloadApplicationSettings,
} from '@konfigyr/hooks/types';

// ─── Validation schemas ───────────────────────────────────────────────────────

const LOOPBACK_HOSTS = ['127.0.0.1', 'localhost', '[::1]'];

export const redirectUriItemSchema = z.string().superRefine((val, ctx) => {
  if (!val.trim()) {
    ctx.addIssue({ code: 'custom', message: 'Redirect URI cannot be blank' });
    return;
  }
  let url: URL;
  try {
    url = new URL(val);
  } catch {
    ctx.addIssue({ code: 'custom', message: 'Must be a valid URL starting with http:// or https://' });
    return;
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    ctx.addIssue({ code: 'custom', message: 'Must start with http:// or https://' });
    return;
  }
  if (url.protocol === 'http:' && !LOOPBACK_HOSTS.includes(url.hostname)) {
    ctx.addIssue({ code: 'custom', message: 'http:// is only permitted for loopback hosts (127.0.0.1, localhost, [::1])' });
  }
});

export const issuerUriSchema = z.string().superRefine((val, ctx) => {
  if (!val) return;
  try {
    const url = new URL(val);
    if (url.protocol !== 'https:') {
      ctx.addIssue({ code: 'custom', message: 'Issuer URI must use https://' });
    }
  } catch {
    ctx.addIssue({ code: 'custom', message: 'Issuer URI must be a valid URL' });
  }
});

export const subjectPatternSchema = z.string().superRefine((val, ctx) => {
  if (val && !val.trim()) {
    ctx.addIssue({ code: 'custom', message: 'Subject pattern cannot be blank or whitespace' });
  }
});

const baseApplicationSchema = z.object({
  name: z.string()
    .nonempty({ message: 'Application name can not be blank' })
    .min(3, { message: 'Application name must be at least 3 characters long' })
    .max(30, { message: 'Application name must be at most 30 characters long' }),
  expiresAt: z.string()
    .refine((val) => !val || !isNaN(Date.parse(val)), {
      message: 'Expiration date must be a valid date',
    }),
  scopes: z.array(z.string()),
  redirectUris: z.array(z.string()),
  issuerUri: z.string(),
  subjectPattern: z.string(),
});

export function createApplicationSchema(type: NamespaceApplicationType) {
  return baseApplicationSchema.superRefine((data, ctx) => {
    if (type === 'AGENT') {
      if (data.redirectUris.length === 0) {
        ctx.addIssue({ code: 'custom', message: 'At least one redirect URI is required', path: ['redirectUris'] });
      } else {
        const seen = new Set<string>();
        data.redirectUris.forEach((uri, index) => {
          if (seen.has(uri)) {
            ctx.addIssue({ code: 'custom', message: 'Redirect URIs must be unique', path: ['redirectUris', index] });
          }
          seen.add(uri);
        });
      }
    }
    if (type === 'WORKLOAD') {
      if (!data.issuerUri) {
        ctx.addIssue({ code: 'custom', message: 'Issuer URI is required', path: ['issuerUri'] });
      } else {
        try {
          const url = new URL(data.issuerUri);
          if (url.protocol !== 'https:') {
            ctx.addIssue({ code: 'custom', message: 'Issuer URI must use https://', path: ['issuerUri'] });
          }
        } catch {
          ctx.addIssue({ code: 'custom', message: 'Issuer URI must be a valid URL', path: ['issuerUri'] });
        }
      }
      if (data.subjectPattern && !data.subjectPattern.trim()) {
        ctx.addIssue({ code: 'custom', message: 'Subject pattern cannot be blank or whitespace', path: ['subjectPattern'] });
      }
    }
  });
}

// ─── Extended form hook ───────────────────────────────────────────────────────
//
// createFormHookContexts() returns module-level singletons — calling it here
// yields the exact same fieldContext/formContext as the base useForm in form.tsx,
// so all field components registered below share the same React context and are
// fully compatible with one another.

const { fieldContext, formContext, useFieldContext, useFormContext } = createFormHookContexts();

function ApplicationNameField() {
  return (
    <FormControl
      label={<FormattedMessage
        defaultMessage="Application name"
        description="Label for the application name form field"
      />}
      description={<FormattedMessage
        defaultMessage="Enter a name for the application."
        description="Help text for the namespace application name field"
      />}
      render={<FormInput />}
    />
  );
}

function ApplicationExpiresAtField() {
  return (
    <FormControl
      label={<FormattedMessage
        defaultMessage="Expiration date"
        description="Label for the application expiration date field"
      />}
      description={<FormattedMessage
        defaultMessage="Select the date when this application should expire."
        description="Help text for the namespace application expiration date field"
      />}
      render={<FormInput type="date" />}
    />
  );
}

function ApplicationScopesFormField({ namespace }: { namespace: Namespace }) {
  const field = useFieldContext<Array<string>>();
  return (
    <ApplicationScopesField
      namespace={namespace}
      value={field.state.value}
      onChange={field.handleChange}
    />
  );
}

function normalizeFieldError(e: unknown): { message?: string } {
  if (typeof e === 'string') return { message: e };
  if (e && typeof e === 'object' && 'message' in e) return e as { message?: string };
  return {};
}

function ApplicationRedirectUrisField() {
  const field = useFieldContext<Array<string>>();
  const form = useFormContext() as any;

  return (
    <FieldSet>
      <FieldLegend variant="label">
        <FormattedMessage
          defaultMessage="Redirect URIs"
          description="Label for the redirect URIs field group"
        />
      </FieldLegend>
      <FieldDescription>
        <FormattedMessage
          defaultMessage="Register the callback URLs where the authorization server may redirect after the user authenticates."
          description="Help text for the redirect URIs field"
        />
      </FieldDescription>
      <FieldGroup className="gap-2">
        {field.state.value.length === 0 && (
          <FieldDescription className="italic">
            <FormattedMessage
              defaultMessage="No redirect URIs registered yet. Add at least one callback URL to continue."
              description="Empty state shown in the redirect URIs field when no URIs have been added"
            />
          </FieldDescription>
        )}

        {field.state.value.map((_, index) => (
          <form.Field key={index} name={`redirectUris[${index}]`} validators={{ onBlur: redirectUriItemSchema }}>
            {(subField: any) => (
              <Field>
                <div className="flex gap-2">
                  <Input
                    value={subField.state.value as string}
                    placeholder="https://example.com/callback"
                    aria-label="Redirect URI"
                    aria-invalid={!subField.state.meta.isValid}
                    onChange={(e) => subField.handleChange(e.target.value)}
                    onBlur={() => subField.handleBlur()}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label="Remove redirect URI"
                    onClick={() => field.removeValue(index)}
                  >
                    <Trash2 size="1rem" />
                  </Button>
                </div>
                <FieldError errors={subField.state.meta.errors.map(normalizeFieldError)} />
              </Field>
            )}
          </form.Field>
        ))}
        <FieldError errors={field.state.meta.errors.map(normalizeFieldError)} />
      </FieldGroup>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="w-fit"
        onClick={() => field.pushValue('')}
      >
        <Plus size="1rem" />
        <FormattedMessage
          defaultMessage="Add redirect URI"
          description="Button label to add a new redirect URI row"
        />
      </Button>
    </FieldSet>
  );
}

function ApplicationIssuerUriField() {
  return (
    <FormControl
      label={<FormattedMessage
        defaultMessage="Issuer URI"
        description="Label for the workload issuer URI field"
      />}
      description={<FormattedMessage
        defaultMessage="The HTTPS URL of the external OIDC token issuer (e.g. your CI/CD platform)."
        description="Help text for the workload issuer URI field"
      />}
      render={<FormInput placeholder="https://token.actions.githubusercontent.com" />}
    />
  );
}

function ApplicationSubjectPatternField() {
  return (
    <FormControl
      label={<FormattedMessage
        defaultMessage="Subject pattern"
        description="Label for the workload subject pattern field"
      />}
      description={<FormattedMessage
        defaultMessage="Optional pattern to restrict which token subjects are accepted."
        description="Help text for the workload subject pattern field"
      />}
      render={<FormInput placeholder="repo:owner/name:ref:refs/heads/main" />}
    />
  );
}

const { useAppForm: useApplicationForm } = createFormHook({
  fieldContext,
  formContext,
  fieldComponents: {
    Control: FormControl,
    Input: FormInput,
    Label,
    SearchInput: FormSearchInput,
    Switch: FormSwitch,
    Textarea: FormTextarea,
    NameField: ApplicationNameField,
    ExpiresAtField: ApplicationExpiresAtField,
    ScopesField: ApplicationScopesFormField,
    RedirectUrisField: ApplicationRedirectUrisField,
    IssuerUriField: ApplicationIssuerUriField,
    SubjectPatternField: ApplicationSubjectPatternField,
  },
  formComponents: {
    Submit: SubmitButton,
  },
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

const extractApplicationScopes = (scopes?: string): Array<string> => {
  if (typeof scopes === 'string') {
    return scopes.split(' ').filter(scope => scope !== '');
  }
  return [];
};

const extractApplicationExpiryDate = (value?: string | number | Date): string => {
  let date: Date | undefined;
  if (typeof value === 'string') date = new Date(value);
  if (typeof value === 'number') date = new Date(value);
  if (value instanceof Date) date = value;
  if (date) {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
  return '';
};

const buildPayload = (
  type: NamespaceApplicationType,
  value: {
    name: string;
    scopes: Array<string>;
    expiresAt: string;
    redirectUris: Array<string>;
    issuerUri: string;
    subjectPattern: string;
  },
): CreateNamespaceApplication => {
  const base = {
    name: value.name,
    type,
    scopes: value.scopes.join(' '),
    expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
  };
  if (type === 'AGENT') {
    return { ...base, settings: { type: 'agent', redirectUris: value.redirectUris } };
  }
  if (type === 'WORKLOAD') {
    return {
      ...base,
      settings: {
        type: 'workload',
        issuerUri: value.issuerUri,
        subjectPattern: value.subjectPattern || undefined,
      },
    };
  }
  return { ...base, settings: null };
};

// ─── Main component ───────────────────────────────────────────────────────────

export function NamespaceApplicationForm({ namespace, namespaceApplication, type, handleSubmit }: {
  namespace: Namespace;
  namespaceApplication?: NamespaceApplication;
  type: NamespaceApplicationType;
  handleSubmit: (namespaceApplication: CreateNamespaceApplication) => void | Promise<void>;
}) {
  const errorNotification = useErrorNotification();

  const schema = useMemo(() => createApplicationSchema(type), [type]);

  const defaultValues = useMemo(() => {
    const agentSettings = namespaceApplication?.settings as AgentApplicationSettings | null;
    const workloadSettings = namespaceApplication?.settings as WorkloadApplicationSettings | null;
    return {
      name: namespaceApplication?.name ?? '',
      scopes: extractApplicationScopes(namespaceApplication?.scopes),
      expiresAt: extractApplicationExpiryDate(namespaceApplication?.expiresAt),
      redirectUris: agentSettings?.redirectUris ?? [''],
      issuerUri: workloadSettings?.issuerUri ?? '',
      subjectPattern: workloadSettings?.subjectPattern ?? '',
    };
  }, [namespaceApplication]);

  const form = useApplicationForm({
    defaultValues,
    validators: {
      onSubmit: schema,
    },
    onSubmit: async ({ value }) => {
      try {
        await handleSubmit(buildPayload(type, value));
      } catch (error) {
        return errorNotification(error);
      }
    },
  });

  const onSubmit = useFormSubmit(form);

  return (
    <form.AppForm>
      <form name="namespace-application-form" className="grid gap-4" onSubmit={onSubmit}>

        <form.AppField name="name" children={(field) => <field.NameField />} />

        <form.AppField name="expiresAt" children={(field) => <field.ExpiresAtField />} />

        {type === 'AGENT' && (
          <form.AppField
            name="redirectUris"
            mode="array"
            children={(field) => <field.RedirectUrisField />}
          />
        )}

        {type === 'WORKLOAD' && (
          <>
            <form.AppField
              name="issuerUri"
              validators={{ onBlur: issuerUriSchema }}
              children={(field) => <field.IssuerUriField />}
            />
            <form.AppField
              name="subjectPattern"
              validators={{ onBlur: subjectPatternSchema }}
              children={(field) => <field.SubjectPatternField />}
            />
          </>
        )}

        <form.AppField name="scopes" children={(field) => <field.ScopesField namespace={namespace} />} />

        <form.Submit>
          {namespaceApplication?.id ? <UpdateNamespaceApplicationLabel /> : <CreateNamespaceApplicationLabel />}
        </form.Submit>
      </form>
    </form.AppForm>
  );
}
