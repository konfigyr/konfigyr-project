import { useCallback, useState} from 'react';
import { MonitorCloud } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  Card, CardAction,
  CardContent, CardFooter,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { ClipboardIconButton } from '@konfigyr/components/clipboard';
import {useNavigate} from '@tanstack/react-router';
import {
  ConfirmNamespaceApplicationDeleteAction,
  ConfirmNamespaceApplicationResetAction,
} from '@konfigyr/components/namespace/applications/confirm-application-action';
import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

export interface ClientSecret {
  clientSecret?: string;
}

export type ApplicationDetailsProps = {
  namespace: Namespace,
  application: NamespaceApplication,
};

type TextProps = {
  value?: string,
};


export function ApplicationDetails ({ namespace, application } : ApplicationDetailsProps) {
  return (
    <Card className="border">
      <CardHeader>
        <ApplicationDetails.Title value={application.name} />
      </CardHeader>
      <CardContent>
        <div className="flex flex-col gap-6">
          <article data-slot="namespace-application-article" className="flex justify-between items-center gap-4">
            <div className="grow">
              <ApplicationDetails.Scopes value={application.scopes} />
              <ApplicationDetails.ClientId value={application.clientId} />
              <ApplicationDetails.ClientSecret value={application.clientSecret} />
            </div>
          </article>
        </div>
      </CardContent>
      <CardFooter className="justify-end border-t">
        <ApplicationDetails.Actions application={application} namespace={namespace} />
      </CardFooter>
    </Card>
  );
}

ApplicationDetails.Title = function Title({ value } : TextProps) {
  return (
    <CardTitle className="flex items-center gap-2">
      <CardIcon>
        <MonitorCloud size="1.25rem"/>
      </CardIcon>
      {value}
    </CardTitle>
  );
};

ApplicationDetails.Actions = function Actions({ namespace, application } : ApplicationDetailsProps) {
  const navigate = useNavigate();

  const onDeleted = useCallback(async (app: NamespaceApplication) => await navigate({
    to: '/namespace/$namespace/applications',
    params: { namespace: namespace.slug },
  }), []);

  const onReset = useCallback((app : NamespaceApplication) => navigate({
    to: '/namespace/$namespace/applications/$id',
    params: { namespace: namespace.slug, id: app.id },
    state: (prev) => ({
      ...prev,
      clientSecret: app.clientSecret,
    }),
  }), []);

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

ApplicationDetails.ClientId = function ClientId({ value } : TextProps) {
  return (
    <>
      <p className="pb-1">
        <span className="font-bold pr-1">
          <FormattedMessage
            defaultMessage="Client ID:"
            description="Client identifier of the token for an application"
          />
        </span>
        {value}
        <span className="pl-5">
          { value ? <ClipboardIconButton text={value} /> : '' }
        </span>
      </p>
    </>
  );
};

ApplicationDetails.Scopes = function Scopes({ value } : TextProps) {
  return (
    <>
      <p className="pb-1">
        <span className="font-bold pr-1">
          <FormattedMessage
            defaultMessage="Scopes:"
            description="Application scopes"
          />
        </span>
        {value}
      </p>
    </>
  );
};

ApplicationDetails.ClientSecret = function ClientSecret({ value } : TextProps) {
  if (!value) {
    return null;
  }

  return (
    <>
      <p className="pb-1">
        <span className="font-bold pr-1">
          <FormattedMessage
            defaultMessage="Client Secret:"
            description="Client secret value display"
          />
        </span>
        {value}
        <span className="pl-5">
          <ClipboardIconButton text={value} />
        </span>
      </p>
      <p className="pt-2 text-red-500">
        <FormattedMessage
          defaultMessage="Make sure to copy your client secret now as you will not be able to see this again."
          description="Client secret warning message"
        />
      </p>
    </>
  );
};
