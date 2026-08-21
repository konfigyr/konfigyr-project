import { useMemo } from 'react';
import { Link } from '@tanstack/react-router';
import { FormattedMessage } from 'react-intl';
import { ChevronRightIcon, ScreenShareOff, VaultIcon } from 'lucide-react';
import { useNamespaceServicesQuery } from '@konfigyr/hooks';
import { ServicesLabel } from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from '@konfigyr/components/ui/avatar';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';

import type { Namespace, Service } from '@konfigyr/hooks/types';

function SkeletonArticle() {
  return (
    <article data-slot="namespace-service-skeleton" className="flex justify-between items-center p-4 gap-4">
      <div className="grow space-y-3">
        <Skeleton className="w-48 h-4" />
        <Skeleton className="w-64 h-4" />
      </div>
      <Skeleton className="w-2 h-4 mr-2" />
    </article>
  );
}

function NamespaceServiceArticle({ service, namespace }: { namespace: Namespace, service: Service }) {
  return (
    <Item
      className="-mx-2"
      render={
        <Link
          to="/namespace/$namespace/services/$service"
          params={{
            namespace: namespace.slug,
            service: service.slug,
          }}>
          <ItemMedia>
            <NamespaceServiceAvatar service={service} />
          </ItemMedia>
          <ItemContent>
            <ItemTitle>
              {service.name}
            </ItemTitle>
            <ItemDescription>
              {service.description}
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

const useServiceAvatar = (service: Service) => useMemo(() => {
  const initials = service.slug.split('-')
    .map(word => word[0])
    .map(char => char.toUpperCase())
    .join('');

  const src = `https://avatar.vercel.sh/${service.slug}.svg?text=${initials}`;

  return [initials, src];
}, [service.slug]);

function NamespaceServiceAvatar({ service }: { service: Service }) {
  const [initials, src] = useServiceAvatar(service);

  return (
    <Avatar className="size-10">
      <AvatarImage src={src} />
      <AvatarFallback>{initials}</AvatarFallback>
    </Avatar>
  );
}

export function NamespaceServices({ namespace }: { namespace: Namespace }) {
  const { data: services, error, isError, isLoading } = useNamespaceServicesQuery(namespace.slug);

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <VaultIcon size="1.25rem"/>
          </CardIcon>
          <ServicesLabel />
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <SkeletonArticle />
        )}

        {isError && (
          <ErrorState error={error} className="border-none" />
        )}

        {services?.length === 0 && (
          <EmptyState
            title={
              <FormattedMessage
                defaultMessage="Your vault has no services yet."
                description="Title for the empty state component in the vault services page."
              />
            }
            description={
              <FormattedMessage
                defaultMessage="Create a new service to get started."
                description="Description for the empty state component in the vault services page."
              />
            }
            icon={<ScreenShareOff />}
          />
        )}

        {services && (
          <ItemGroup>
            {services.map(service => (
              <NamespaceServiceArticle
                key={service.id}
                service={service}
                namespace={namespace}
              />
            ))}
          </ItemGroup>
        )}
      </CardContent>
    </Card>
  );
}
