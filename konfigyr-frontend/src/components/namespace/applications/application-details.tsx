import { useCallback, useId } from 'react';
import { MonitorCloud } from 'lucide-react';
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

import type { ReactNode } from 'react';
import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

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

export function ApplicationDetails ({ namespace, application }: ApplicationDetailsProps) {
  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <MonitorCloud size="1.25rem"/>
          </CardIcon>
          {application.name}
        </CardTitle>
        <CardDescription>
          <FormattedMessage
            defaultMessage="Use the credentials below to authenticate this application against the Konfigyr Identity Provider and access the API according to its assigned scopes."
            description="Description of the application details card"
          />
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
