import {createContext, useCallback, useContext, useState} from 'react';
import { MonitorCloud } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card, CardAction,
  CardContent, CardFooter,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  DeleteNamespaceApplicationLabel,
} from '@konfigyr/components/namespace/applications/messages';
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

type ApplicationDetailsProps = {
  namespace: Namespace,
  application: NamespaceApplication,
};

const useApplicationDetails = () => {
  const context = useContext(ApplicationDetailsContext);

  if (!context) {
    throw new Error('useApplicationDetails must be used within ApplicationDetailsProvider');
  }
  return context;
};

const ApplicationDetailsContext = createContext<ApplicationDetailsProps | undefined>(undefined);

export function ApplicationDetails ({ namespace, application } : ApplicationDetailsProps) {
  return (
    <ApplicationDetailsContext.Provider value={{ namespace, application }} >
      <Card className="border">
        <CardHeader>
          <ApplicationDetails.Title />
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6">
            <article data-slot="namespace-application-article" className="flex justify-between items-center gap-4">
              <div className="grow">
                <ApplicationDetails.Scopes />
                <ApplicationDetails.ClientId />
                <ApplicationDetails.ClientSecret />
              </div>
            </article>
          </div>
        </CardContent>
        <CardFooter className="justify-end border-t">
          <ApplicationDetails.Actions />
        </CardFooter>
      </Card>
    </ApplicationDetailsContext.Provider>
  );
}

ApplicationDetails.Title = function Title() {
  const { application } = useApplicationDetails();
  return (
    <CardTitle className="flex items-center gap-2">
      <CardIcon>
        <MonitorCloud size="1.25rem"/>
      </CardIcon>
      {application.name}
    </CardTitle>
  );
};

ApplicationDetails.Actions = function Actions() {
  const { application, namespace } = useApplicationDetails();

  const navigate = useNavigate();

  const [removing, setRemoving] = useState<NamespaceApplication | undefined>();
  const [resetting, setResetting] = useState<NamespaceApplication | undefined>();

  const onCloseRemoving = useCallback(() => setRemoving(undefined), []);
  const onCloseResetting = useCallback(() => setResetting(undefined), []);

  const onDeleted = useCallback(() => navigate({
    to: '/namespace/$namespace/applications',
    params: { namespace: namespace.slug },
  }), []);

  const onReset = useCallback(() => {}, []);

  return (
    <>
      <CardAction className="pr-5">
        <Button variant="secondary" onClick={() => setResetting(application)}>
          <FormattedMessage
            defaultMessage="Reset application"
            description="Button label that triggers application reset confirmation dialog when clicked"
          />
        </Button>
      </CardAction>
      <CardAction>
        <Button variant="destructive" onClick={() => setRemoving(application)}>
          <DeleteNamespaceApplicationLabel/>
        </Button>
      </CardAction>

      <ConfirmNamespaceApplicationResetAction
        namespace={namespace}
        application={resetting}
        onClose={onCloseResetting}
        onSuccess={onDeleted}
      />

      <ConfirmNamespaceApplicationDeleteAction
        namespace={namespace}
        application={removing}
        onClose={onCloseRemoving}
        onSuccess={onReset}
      />
    </>
  );
};

ApplicationDetails.ClientId = function ClientId() {
  const { application } = useApplicationDetails();
  return (
    <>
      <p className="pb-1">
        <span className="font-bold pr-1">
          <FormattedMessage
            defaultMessage="Client ID:"
            description="Client identifier of the token for an application"
          />
        </span>
        {application.clientId}
        <span className="pl-5">
          <ClipboardIconButton text={application.clientId} />
        </span>
      </p>
    </>
  );
};

ApplicationDetails.Scopes = function Scopes() {
  const { application } = useApplicationDetails();
  return (
    <>
      <p className="pb-1">
        <span className="font-bold pr-1">
          <FormattedMessage
            defaultMessage="Scopes:"
            description="Application scopes"
          />
        </span>
        {application.scopes}
      </p>
    </>
  );
};

ApplicationDetails.ClientSecret = function ClientSecret() {
  const { application } = useApplicationDetails();

  if (!application.clientSecret) {
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
        {application.clientSecret}
        <span className="pl-5">
          <ClipboardIconButton text={application.clientSecret}/>
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
