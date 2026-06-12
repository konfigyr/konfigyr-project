import { useCallback, useId } from 'react';
import { FormattedMessage } from 'react-intl';
import { useNavigate } from '@tanstack/react-router';
import { ClipboardIconButton } from '@konfigyr/components/clipboard';
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  Field,
  FieldDescription,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from '@konfigyr/components/ui/input-group';
import {
  ConfirmNamespaceApplicationDeleteAction,
  ConfirmNamespaceApplicationResetAction,
} from './confirm-application-action';
import {
  ApplicationTypeBadge,
  ApplicationTypeIcon,
  ApplicationTypeInstruction,
} from './application-type';

import type { ReactNode } from 'react';
import type { AgentApplicationSettings, Namespace, NamespaceApplication, WorkloadApplicationSettings } from '@konfigyr/hooks/types';

export type ApplicationDetailsProps = {
  namespace: Namespace,
  application: NamespaceApplication,
};



function FieldGroup({ label, value, description, copy = true }: {
  label: string | ReactNode,
  value?: string,
  description: string | ReactNode,
  copy?: boolean,
}) {
  const id = useId();

  if (!value) {
    return null;
  }

  const labelId = `${id}-label`;
  const descriptionId = `${id}-description`;

  return (
    <Field>
      <FieldLabel id={labelId}>
        {label}
      </FieldLabel>
      <InputGroup>
        <InputGroupInput
          value={value}
          aria-labelledby={labelId}
          aria-describedby={descriptionId}
          readOnly
        />
        {copy && (
          <InputGroupAddon align="inline-end">
            <ClipboardIconButton text={value} size="xs" variant="link" className="cursor-pointer" />
          </InputGroupAddon>
        )}
      </InputGroup>
      <FieldDescription id={descriptionId}>
        {description}
      </FieldDescription>
    </Field>
  );
}

function CopyableUri({ value }: { value: string }) {
  const id = useId();
  return (
    <InputGroup>
      <InputGroupInput
        value={value}
        aria-labelledby={id}
        readOnly
      />
      <InputGroupAddon align="inline-end">
        <ClipboardIconButton text={value} size="xs" variant="link" className="cursor-pointer" />
      </InputGroupAddon>
    </InputGroup>
  );
}

export function ApplicationDetails ({ namespace, application }: ApplicationDetailsProps) {
  const agentSettings = application.settings as AgentApplicationSettings | null;
  const workloadSettings = application.settings as WorkloadApplicationSettings | null;

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <ApplicationTypeIcon type={application.type} />
          </CardIcon>
          {application.name}
          <ApplicationTypeBadge type={application.type} />
        </CardTitle>
        <CardDescription>
          <ApplicationTypeInstruction type={application.type} />
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4">
        <FieldGroup
          value={application.clientId}
          label={<FormattedMessage
            defaultMessage="Client ID"
            description="Label for OAuth application client identifier of the token for an application"
          />}
          description={<FormattedMessage
            defaultMessage="Public identifier of this application. Use it in your OAuth2 client configuration when requesting access tokens."
            description="Description for OAuth application client identifier of the token for an application"
          />}
        />

        {application.type === 'SERVICE_ACCOUNT' && (
          <FieldGroup
            value={application.clientSecret ?? '***********'}
            copy={!!application.clientSecret}
            label={<FormattedMessage
              defaultMessage="Client secret"
              description="Label for OAuth application client secret for an application"
            />}
            description={application.clientSecret ? <FormattedMessage
              defaultMessage="Confidential credential used to authenticate this application with the Identity Provider. Store it securely. You can reset it at any time if compromised."
              description="Description for OAuth application client secret for an application"
            /> : <FormattedMessage
              defaultMessage="For security reasons, the client secret is never persisted. It is shown only once upon creation or reset. If lost, you must reset the application to generate a new secret."
              description="Description for OAuth application client secret for an application when it is not available and should inform the user to reset the application"
            />}
          />
        )}

        <FieldGroup
          value={application.scopes}
          label={<FormattedMessage
            defaultMessage="Scopes"
            description="Label for OAuth application scopes for an application"
          />}
          description={<FormattedMessage
            defaultMessage="Granted OAuths scopes for your application"
            description="Description for OAuth application scopes for an application"
          />}
        />

        {application.type === 'AGENT' && agentSettings?.redirectUris && agentSettings.redirectUris.length > 0 && (
          <Field>
            <FieldLabel>
              <FormattedMessage
                defaultMessage="Redirect URIs"
                description="Label for registered redirect URIs in application details"
              />
            </FieldLabel>
            <ul className="grid gap-2">
              {agentSettings.redirectUris.map((uri) => (
                <li key={uri}>
                  <CopyableUri value={uri} />
                </li>
              ))}
            </ul>
            <FieldDescription>
              <FormattedMessage
                defaultMessage="Registered callback URLs that the authorization server may redirect to after authentication."
                description="Description for redirect URIs in application details"
              />
            </FieldDescription>
          </Field>
        )}

        {application.type === 'WORKLOAD' && workloadSettings?.issuerUri && (
          <>
            <FieldGroup
              value={workloadSettings.issuerUri}
              label={<FormattedMessage
                defaultMessage="Issuer URI"
                description="Label for the OIDC token issuer URI in workload application details"
              />}
              description={<FormattedMessage
                defaultMessage="The OIDC token issuer URL that this workload identity trusts for token exchange."
                description="Description for the issuer URI in workload application details"
              />}
            />
            {workloadSettings.subjectPattern && (
              <FieldGroup
                value={workloadSettings.subjectPattern}
                label={<FormattedMessage
                  defaultMessage="Subject pattern"
                  description="Label for the subject pattern in workload application details"
                />}
                description={<FormattedMessage
                  defaultMessage="The pattern used to restrict which token subjects are accepted."
                  description="Description for the subject pattern in workload application details"
                />}
              />
            )}
          </>
        )}
      </CardContent>
      <CardFooter className="justify-end border-t">
        <ApplicationDetails.Actions application={application} namespace={namespace} />
      </CardFooter>
    </Card>
  );
}

ApplicationDetails.Actions = function Actions({ namespace, application }: ApplicationDetailsProps) {
  const navigate = useNavigate();

  const onDeleted = useCallback(async () => await navigate({
    to: '/namespace/$namespace/applications',
    params: { namespace: namespace.slug },
  }), [namespace.slug]);

  const onReset = useCallback((app: NamespaceApplication) => navigate({
    to: '/namespace/$namespace/applications/$id',
    params: { namespace: namespace.slug, id: app.id },
    state: (prev) => ({
      ...prev,
      clientSecret: app.clientSecret,
    }),
  }), [namespace.slug]);

  return (
    <>
      <CardAction className="pr-5">
        <ConfirmNamespaceApplicationResetAction
          namespace={namespace}
          application={application}
          disabled={application.type !== 'SERVICE_ACCOUNT'}
          onConfirm={onReset}
        />
      </CardAction>
      <CardAction>
        <ConfirmNamespaceApplicationDeleteAction
          namespace={namespace}
          application={application}
          onConfirm={onDeleted}
        />
      </CardAction>
    </>
  );
};
