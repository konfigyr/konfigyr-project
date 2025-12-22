import { useCallback, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { PlusIcon, ServerIcon, ServerOffIcon } from 'lucide-react';
import { useNamespaceServicesQuery } from '@konfigyr/hooks';
import { CreateServiceForm } from '@konfigyr/components/namespace/service/service-form';
import { ErrorState } from '@konfigyr/components/error';
import { Button } from '@konfigyr/components/ui/button';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@konfigyr/components/ui/sidebar';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import { Link, useNavigate } from '@tanstack/react-router';

import type { Namespace, Service } from '@konfigyr/hooks/types';

function ServicesMenu({ namespace }: { namespace: Namespace}) {
  const { data: services, isPending, isError, error } = useNamespaceServicesQuery(namespace.slug);

  if (isPending) {
    return (
      <Skeleton className="p-2 text-xs text-muted-foreground">
        <FormattedMessage
          defaultMessage="Loading services..."
          description="Loading message shown when services are being fetched"
        />
      </Skeleton>
    );
  }

  if (isError) {
    return <ErrorState error={error} />;
  }

  if (services.length === 0) {
    return (
      <EmptyState
        title={<FormattedMessage
          defaultMessage="No services found"
          description="Empty state title used when a namespace has no services defined"
        />}
        description={<FormattedMessage
          defaultMessage="There are currently no services for this namespace. Why don't you create one?"
          description="Empty state description used when a namespace has no services defined"
        />}
        icon={<ServerOffIcon />}
        size="sm"
      />
    );
  }

  return (
    <SidebarMenu>
      {services.map(service => (
        <SidebarMenuItem key={service.id}>
          <SidebarMenuButton asChild>
            <Link
              to="/namespace/$namespace/services/$service"
              params={{ namespace: namespace.slug, service: service.slug }}
            >
              {service.slug}
            </Link>
          </SidebarMenuButton>
        </SidebarMenuItem>
      ))}
    </SidebarMenu>
  );
}

function ServiceDialog({ namespace }: { namespace: Namespace }) {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const onCreate = useCallback(async (service: Service) => {
    setOpen(false);

    await navigate({
      to: '/namespace/$namespace/services/$service',
      params: { namespace: namespace.slug, service: service.slug },
    });
  }, [navigate]);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm">
          <PlusIcon size="1rem"/>
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogTitle>
          <FormattedMessage
            defaultMessage="Create new service"
            description="Title of the modal that is shown when user tries to create a new service"
          />
        </DialogTitle>
        <DialogDescription>
          <FormattedMessage
            defaultMessage="Register your Spring Boot service within this namespace to begin managing its environment-specific configurations."
            description="Modal description text that is shown when user tries to create a new service"
          />
        </DialogDescription>
        <CreateServiceForm namespace={namespace} onCreate={onCreate}/>
      </DialogContent>
    </Dialog>
  );
}

export function NamespaceServicesNavigationMenu({ namespace }: { namespace: Namespace}) {
  return (
    <SidebarGroup>
      <SidebarGroupContent>
        <SidebarGroupLabel className="flex items-center gap-2">
          <ServerIcon />
          <span className="flex-grow">Services</span>
          <ServiceDialog namespace={namespace} />
        </SidebarGroupLabel>
        <ServicesMenu namespace={namespace} />
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
