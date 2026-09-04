import { useCallback, useEffect, useRef, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import {
  BoxesIcon,
  ChevronRight,
  FolderIcon,
  GitPullRequestIcon,
  HashIcon,
  PlusIcon,
  ServerIcon,
  ServerOffIcon,
  SettingsIcon,
} from 'lucide-react';
import { useGetProfiles, useNamespaceServicesQuery } from '@konfigyr/hooks';
import { CreateServiceForm } from '@konfigyr/components/namespace/service/service-form';
import { ChangeRequestsLabel, ServiceManifestLabel } from '@konfigyr/components/namespace/service/messages';
import { ErrorState } from '@konfigyr/components/error';
import {
  OverviewLabel,
  ServicesLabel,
  SettingsLabel,
  VaultModuleLabel,
} from '@konfigyr/components/messages';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@konfigyr/components/ui/collapsible';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarSeparator,
} from '@konfigyr/components/ui/sidebar';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import { cn } from '@konfigyr/components/utils';
import { Link, useMatches, useNavigate } from '@tanstack/react-router';

import type { Namespace, Service } from '@konfigyr/hooks/types';

function ProfilesMenu({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { data, error, isPending, isError } = useGetProfiles(namespace, service);

  return (
    <>
      {isPending && (
        <SidebarMenuSubItem className="text-xs text-muted-foreground text-center">
          <Skeleton className="p-2 text-xs text-muted-foreground">
            <FormattedMessage
              defaultMessage="Loading profiles..."
              description="Loading message shown when profiles are being fetched for a service."
            />
          </Skeleton>
        </SidebarMenuSubItem>
      )}
      {isError && (
        <ErrorState error={error} />
      )}
      {data?.map(profile => (
        <SidebarMenuSubItem>
          <SidebarMenuSubButton size="sm" render={(
            <Link
              to="/namespace/$namespace/services/$service/profiles/$profile"
              params={{ namespace: namespace.slug, service: service.slug, profile: profile.slug }}
              activeProps={{ 'data-active': true }}
            >
              <HashIcon className="size-3"/>
              <code>{profile.slug}</code>
            </Link>
          )} />
        </SidebarMenuSubItem>
      ))}
      <SidebarMenuSubItem>
        <SidebarMenuSubButton size="sm" render={(
          <Link
            to="/namespace/$namespace/services/$service/create-profile"
            params={{ namespace: namespace.slug, service: service.slug }}
            activeProps={{ 'data-active': true }}
          >
            <PlusIcon />
            Create profile
          </Link>
        )} />
      </SidebarMenuSubItem>
    </>
  );
}

function ServiceMenu({ namespace, service, active }: { namespace: Namespace, service: Service, active?: string }) {
  const ref = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState<boolean | null>(null);
  const isOpened = open ?? active === service.slug;

  useEffect(() => {
    if (service.slug === active) {
      ref.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [active, service.slug]);

  return (
    <Collapsible
      ref={ref}
      key={service.id}
      className="group/collapsible"
      open={isOpened}
      onOpenChange={setOpen}
    >
      <SidebarMenuItem>
        <CollapsibleTrigger render={(
          <SidebarMenuButton
            tooltip={service.slug}
            className={cn(active === service.slug && 'text-primary')}
          >
            <FolderIcon />
            {service.slug}
            <ChevronRight className="ml-auto transition-transform group-data-open/collapsible:rotate-90"/>
          </SidebarMenuButton>
        )} />
        <CollapsibleContent>
          <SidebarMenuSub>
            <ProfilesMenu
              namespace={namespace}
              service={service}
            />
            <SidebarSeparator />
            <SidebarMenuSubItem>
              <SidebarMenuSubButton size="sm" render={(
                <Link
                  to="/namespace/$namespace/services/$service/requests" params={{ namespace: namespace.slug, service: service.slug }}
                  activeProps={{ 'data-active': true }}
                >
                  <GitPullRequestIcon />
                  <ChangeRequestsLabel />
                </Link>
              )} />
            </SidebarMenuSubItem>
            <SidebarMenuSubItem>
              <SidebarMenuSubButton size="sm" render={(
                <Link
                  to="/namespace/$namespace/services/$service/manifest" params={{ namespace: namespace.slug, service: service.slug }}
                  activeProps={{ 'data-active': true }}
                >
                  <BoxesIcon />
                  <ServiceManifestLabel />
                </Link>
              )} />
            </SidebarMenuSubItem>
            <SidebarMenuSubItem>
              <SidebarMenuSubButton size="sm" render={(
                <Link
                  to="/namespace/$namespace/services/$service/settings" params={{ namespace: namespace.slug, service: service.slug }}
                  activeProps={{ 'data-active': true }}
                >
                  <SettingsIcon />
                  <SettingsLabel />
                </Link>
              )} />
            </SidebarMenuSubItem>
          </SidebarMenuSub>
        </CollapsibleContent>
      </SidebarMenuItem>
    </Collapsible>
  );
}

function ServicesMenu({ namespace }: { namespace: Namespace }) {
  const { data: services, isPending, isError, error } = useNamespaceServicesQuery(namespace.slug);
  const matches = useMatches();
  const match = matches.find(it => it.routeId === '/_authenticated/namespace/$namespace/services/$service');

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
    <>
      {services.map(service => (
        <ServiceMenu
          key={service.id}
          namespace={namespace}
          service={service}
          active={match?.params.service}
        />
      ))}
    </>
  );
}

function ServiceDialog({ namespace }: { namespace: Namespace }) {
  const t = useIntl();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const createServiceLabel = t.formatMessage({
    defaultMessage: 'Create new service',
    description: 'Label for the button that opens the modal that is shown when user tries to create a new service',
  });

  const onCreate = useCallback(async (service: Service) => {
    setOpen(false);

    await navigate({
      to: '/namespace/$namespace/services/$service',
      params: { namespace: namespace.slug, service: service.slug },
    });
  }, [navigate]);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger
        render={
          <SidebarMenuButton>
            <PlusIcon />
            {createServiceLabel}
          </SidebarMenuButton>
        }
      />
      <DialogContent>
        <DialogTitle>
          {createServiceLabel}
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

export function NamespaceServicesNavigationMenu({ namespace }: { namespace: Namespace }) {
  return (
    <>
      <SidebarGroup>
        <SidebarGroupContent>
          <SidebarGroupLabel className="flex items-center gap-2">
            <VaultModuleLabel />
          </SidebarGroupLabel>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton render={
                <Link
                  to="/namespace/$namespace/services"
                  params={{ namespace: namespace.slug }}
                  activeProps={{ 'data-active': true }}
                  activeOptions={{ exact: true }}
                >
                  <ServerIcon />
                  <OverviewLabel />
                </Link>
              } />
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
        <SidebarGroupContent>
          <SidebarGroupLabel>
            <ServicesLabel />
          </SidebarGroupLabel>
          <SidebarMenu>
            <ServicesMenu namespace={namespace} />

            <SidebarMenuItem>
              <ServiceDialog namespace={namespace} />
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </>
  );
}
