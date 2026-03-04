import { FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { ChevronRightIcon, MonitorCloud, ScreenShareOff } from 'lucide-react';
import { useGetNamespaceApplications } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { CreateExpirationDateLabel } from './messages';

import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

export function NamespaceApplicationArticle({ application, namespace }: {
  namespace: Namespace,
  application: NamespaceApplication;
}) {
  return (
    <Item asChild>
      <Link
        to="/namespace/$namespace/applications/$id"
        params={{
          namespace: namespace.slug,
          id: application.id,
        }}>
        <ItemContent>
          <ItemTitle>
            {application.name}
          </ItemTitle>
          <ItemDescription>
            <CreateExpirationDateLabel expiresAt={application.expiresAt} />
          </ItemDescription>
        </ItemContent>
        <ItemActions>
          <ChevronRightIcon className="size-4" />
        </ItemActions>
      </Link>
    </Item>
  );
}

export function NamespaceApplications({ namespace }: { namespace: Namespace }) {
  const { data: applications, error, isPending, isError } = useGetNamespaceApplications(namespace.slug);

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
          {isPending && (
            <EmptyState
              title="Namespage applications"
              description="Namespace applications are loading. Please wait"
              icon={<MonitorCloud />}
            />
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

          {applications && (
            <ItemGroup className="-mx-2">
              {applications.map(app => (
                <NamespaceApplicationArticle
                  key={app.id}
                  application={app}
                  namespace={namespace}
                />
              ))}
            </ItemGroup>
          )}
        </CardContent>
      </Card>
    </>
  );
}
