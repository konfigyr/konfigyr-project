import { useCallback, useState } from 'react';
import {MonitorCloud, ScreenShareOff} from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import {
  useGetNamespaceApplications,
  useRemoveNamespaceApplication,
} from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { CreateExpirationDateLabel } from '@konfigyr/components/namespace/applications/messages';
import { Link } from '@tanstack/react-router';
import {
  ConfirmNamespaceApplicationDeleteAction,
} from '@konfigyr/components/namespace/applications/confirm-application-action';
import type { Namespace, NamespaceApplication} from '@konfigyr/hooks/types';

export interface NamespaceApplicationArticleProps {
  namespace: Namespace,
  application: NamespaceApplication;
  onRemove: (member: NamespaceApplication) => void;
}

export function NamespaceApplicationArticle({ application, namespace, onRemove }: NamespaceApplicationArticleProps) {
  return (
    <article data-slot="namespace-application-article" className="flex justify-between items-center gap-4">

      <div className="grow">
        <p className="font-medium">
          <Link
            to="/namespace/$namespace/applications/$id"
            params={{
              namespace: namespace.slug,
              id: application.id,
            }}>
            {application.name}
          </Link>
        </p>
        <p className="text-sm text-muted-foreground">
          <CreateExpirationDateLabel expiresAt={application.expiresAt} />
        </p>
      </div>

      <div>
        <Button variant="destructive" onClick={() => onRemove(application)}>
          <FormattedMessage
            defaultMessage="Delete application"
            description="Button label that triggers application delete confirmation dialog when clicked"
          />
        </Button>
      </div>
    </article>
  );
}

export function NamespaceApplications({ namespace }: { namespace: Namespace }) {
  const { data: applications, error, isPending, isError } = useGetNamespaceApplications(namespace.slug);

  const [removing, setRemoving] = useState<NamespaceApplication | undefined>();

  const onClose = useCallback(() => setRemoving(undefined), []);
  const onDeleted = useCallback(() => {}, []);

  return (
    <>
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CardIcon>
              <MonitorCloud size="1.25rem"/>
            </CardIcon>
            <FormattedMessage
              defaultMessage="Namespace applications"
              description="Title used in the card that lists all namespace applications."
            />
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6">
            {isPending && (
              <article data-slot="namespace-applications-skeleton">
                <EmptyState
                  title="Namespage applications"
                  description="Namespace applications are loading. Please wait"
                  icon={<MonitorCloud />}
                />
              </article>
            )}

            {isError && (
              <ErrorState error={error} className="border-none" />
            )}

            {applications?.length === 0 && (
              <EmptyState
                title="Your namespace has no applications yet."
                description="Create an application in this namespace to get started."
                icon={<ScreenShareOff />}
              />
            )}

            {applications?.map(app => (
              <NamespaceApplicationArticle key={app.id} application={app} onRemove={setRemoving} namespace={namespace} />
            ))}
          </div>
        </CardContent>
      </Card>

      <ConfirmNamespaceApplicationDeleteAction
        namespace={namespace}
        application={removing}
        onClose={onClose}
        onSuccess={onDeleted}
      />
    </>
  );
}
