import { useCallback, useState } from 'react';
import {EllipsisVerticalIcon, MonitorCloud, MonitorIcon, ScreenShareOff} from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  DeleteNamespaceApplicationLabel,
} from '@konfigyr/components/namespace/applications/messages';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import {useNavigate} from '@tanstack/react-router';
import {ConfirmNamespaceApplicationAction} from '@konfigyr/components/namespace/applications/confirm-application-action';
import {useRemoveNamespaceApplication, useResetNamespaceApplication} from '@konfigyr/hooks';
import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

type ClientSecretProps = {
  clientSecret: string;
};

type NamespaceApplicationDetailsProps = {
  namespace: Namespace,
  namespaceApplication: NamespaceApplication,
  showActions?:boolean,
};


export function ClientSecret({ clientSecret }: ClientSecretProps) {
  return (
    <>
      <p className="pb-1">
        <FormattedMessage
          defaultMessage="<b>Client Secret:</b> {clientSecret}"
          description="Client secret value display"
          values={{
            clientSecret,
            b: (chunks) => <strong>{chunks}</strong>,
          }}
        />

        <p className="pt-2 text-red-500">
          <FormattedMessage
            defaultMessage="Make sure to copy your client secret now as you will not be able to see this again."
            description="Client secret warning message"
          />
        </p>
      </p>
    </>)
  ;
}

export function NamespaceApplicationDetails({namespace, namespaceApplication, showActions = false}: NamespaceApplicationDetailsProps) {
  const navigate = useNavigate();

  const [clientSecret, setClientSecret] = useState<string>(namespaceApplication.clientSecret);
  const [removing, setRemoving] = useState<NamespaceApplication | undefined>();
  const [resetting, setResetting] = useState<NamespaceApplication | undefined>();

  const {
    isPending: isPendingRemoving,
    mutateAsync: removeNamespaceApplication,
  } = useRemoveNamespaceApplication(namespace.slug);
  const {
    isPending: isPendingResetting,
    mutateAsync: resetNamespaceApplication,
  } = useResetNamespaceApplication(namespace.slug);

  const onCloseRemoving = useCallback(() => setRemoving(undefined), []);
  const onCloseResetting = useCallback(() => setResetting(undefined), []);

  const onConfirmResetting = async (id: string) => {
    const app = await resetNamespaceApplication(id);
    setClientSecret(app.clientSecret);
  };

  const onConfirmRemoving = async (id: string) => {
    await removeNamespaceApplication(id);
    await navigate({
      to: '/namespace/$namespace/applications',
      params: { namespace: namespace.slug },
    });
  };

  return (
    <>
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CardIcon>
              <MonitorCloud size="1.25rem"/>
            </CardIcon>
            {namespaceApplication.name}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6">
            <article data-slot="namespace-application-article" className="flex justify-between items-center gap-4">
              <div className="grow">
                <p className="pb-1">
                  <FormattedMessage
                    defaultMessage="<b>Scopes:</b> {scopes}"
                    description="Expiration date of the token for an application"
                    values={{
                      scopes: namespaceApplication.scopes,
                      b: (chunks) => <strong>{chunks}</strong>,
                    }}
                  />
                </p>
                <p className="pb-1">
                  <FormattedMessage
                    defaultMessage="<b>Client ID:</b> {clientId}"
                    description="Expiration date of the token for an application"
                    values={{
                      clientId: namespaceApplication.clientId,
                      b: (chunks) => <strong>{chunks}</strong>,
                    }}
                  />
                </p>
                { clientSecret ?
                  <ClientSecret clientSecret={clientSecret} />
                  :
                  ''
                }
              </div>
              { showActions &&
                <div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" aria-label="More options">
                        <EllipsisVerticalIcon />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-48">
                      <DropdownMenuItem onClick={() => setResetting(namespaceApplication)}>
                        <FormattedMessage
                          defaultMessage="Reset application"
                          description="Button label that triggers application reset confirmation dialog when clicked"
                        />
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => setRemoving(namespaceApplication)}>
                        <DeleteNamespaceApplicationLabel/>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              }
            </article>
          </div>
        </CardContent>
      </Card>

      <ConfirmNamespaceApplicationAction
        action={'reset'}
        isPending={isPendingResetting}
        application={resetting}
        onClose={onCloseResetting}
        onConfirm={onConfirmResetting}
      />

      <ConfirmNamespaceApplicationAction
        action={'remove'}
        isPending={isPendingRemoving}
        application={removing}
        onClose={onCloseRemoving}
        onConfirm={onConfirmRemoving}
      />
    </>
  );
}
