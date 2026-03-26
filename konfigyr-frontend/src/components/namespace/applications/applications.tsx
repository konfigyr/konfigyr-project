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
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { CreateExpirationDateLabel } from './messages';

import type { Namespace, NamespaceApplication } from '@konfigyr/hooks/types';

function SkeletonArticle() {
  return (
    <article data-slot="namespace-application-skeleton" className="flex justify-between items-center p-4 gap-4">
      <div className="grow space-y-3">
        <Skeleton className="w-48 h-4" />
        <Skeleton className="w-64 h-4" />
      </div>
      <Skeleton className="w-2 h-4 mr-2" />
    </article>
  );
}

export function NamespaceApplicationArticle({ application, namespace }: {
  namespace: Namespace,
  application: NamespaceApplication;
}) {
  return (
    <Item
      className="-mx-2"
      render={
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
      }
    />
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
            <SkeletonArticle />
          )}

          {isError && (
            <ErrorState error={error} className="border-none" />
          )}

          {applications?.length === 0 && (
            <EmptyState
              title={
                <FormattedMessage
                  defaultMessage="Your namespace has no applications yet."
                  description="Title for the empty state component in the namespace applications page."
                />
              }
              description={
                <FormattedMessage
                  defaultMessage="Create an application in this namespace to get started."
                  description="Description for the empty state component in the namespace applications page."
                />
              }
              icon={<ScreenShareOff />}
            />
          )}

          {applications && (
            <ItemGroup>
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
