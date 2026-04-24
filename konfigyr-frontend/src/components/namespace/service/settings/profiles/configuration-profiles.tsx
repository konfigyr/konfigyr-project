import { Item, ItemActions, ItemContent, ItemDescription, ItemGroup, ItemTitle } from '@konfigyr/components/ui/item';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { ErrorState } from '@konfigyr/components/error';
import { useGetProfiles, useUpdateProfile } from '@konfigyr/hooks';
import React, { useCallback, useState } from 'react';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { useNavigate } from '@tanstack/react-router';
import { PolicyAlertIcon } from '@konfigyr/components/vault/profile/policy-alert';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { EllipsisVerticalIcon, PencilIcon } from 'lucide-react';
import {
  DeleteConfigurationProfileAlert,
} from '@konfigyr/components/namespace/service/settings/profiles/delete-profile-alert';
import {
  UpdateConfigurationProfilePolicyDialog,
} from '@konfigyr/components/namespace/service/settings/profiles/update-policy-dialog';
import { InlineEdit, InlineEditInput, InlineEditPlaceholder } from '@konfigyr/components/ui/inline-edit';
import type { Profile } from '@konfigyr/hooks/vault/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

export type ProfileItemProps = {
  namespace: Namespace,
  service: Service,
  profile: Profile,
  onEdit: (profile: Profile) => void
  onRemove: (profile: Profile) => void
};

export function ServiceConfigurationProfiles ({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { data: profiles, error, isPending, isError } = useGetProfiles(namespace, service);
  const navigate = useNavigate();

  const [updating, setUpdating] = useState<Profile | undefined>();
  const [removing, setRemoving] = useState<Profile | undefined>();

  const onCloseUpdatePolicyDialog = useCallback(() => setUpdating(undefined), []);
  const onCloseRemoveProfileAlert = useCallback(() => setRemoving(undefined), []);

  const handleProfileClick = (e: React.MouseEvent<HTMLSpanElement>) => {
    e.preventDefault();
    navigate({
      to: '/namespace/$namespace/services/$service',
      params: { namespace: namespace.slug, service: service.slug },
    });
  };

  return (
    <>
      <Card className="border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FormattedMessage
              defaultMessage="Configuration profiles"
              description="Title for theconfiguration profiles component"
            />
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isPending && (
            <SkeletonLoader/>
          )}

          {isError && (
            <ErrorState error={error} className="border-none"/>
          )}

          {profiles?.length === 0 && (
            <EmptyState
              title={
                <FormattedMessage
                  defaultMessage="Your service has no configuration profiles yet."
                  description="Title for the empty state component"
                />
              }
              description={
                <FormattedMessage
                  defaultMessage="Create a new <link>configuration profile</link> to get started with this service."
                  description="Description for the empty state component"
                  values={{
                    link: (chunks) => (
                      <a onClick={handleProfileClick} style={{ cursor: 'pointer' }}>
                        {chunks}
                      </a>
                    ),
                  }}
                />
              }
            />
          )}

          {profiles && (
            <ItemGroup>
              {profiles.map(profile => (
                <ProfileItem
                  key={profile.id}
                  namespace={namespace}
                  service={service}
                  profile={profile}
                  onEdit={setUpdating}
                  onRemove={setRemoving}
                />
              ))}
            </ItemGroup>
          )}
        </CardContent>
      </Card>

      <DeleteConfigurationProfileAlert
        namespace={namespace}
        service={service}
        profile={removing}
        onClose={onCloseRemoveProfileAlert}
      />

      <UpdateConfigurationProfilePolicyDialog
        namespace={namespace}
        service={service}
        profile={updating}
        onClose={onCloseUpdatePolicyDialog}
      />
    </>
  );
}

export function ProfileItem ({ namespace, service, profile, onEdit, onRemove }: ProfileItemProps) {
  return (
    <Item variant="list">
      <div>
        <PolicyAlertIcon profile={profile}/>
      </div>
      <ItemContent>
        <ItemTitle>
          <ChangeProfileName namespace={namespace} service={service} profile={profile} onError={() => {}}/>
        </ItemTitle>
        <ItemDescription>
          <ChangeProfileDescription namespace={namespace} service={service} profile={profile} onError={() => {}}/>
        </ItemDescription>
      </ItemContent>

      <ItemActions>
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <Button variant="ghost" aria-label="More options">
                <EllipsisVerticalIcon/>
              </Button>
            }
          />
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem onClick={() => onEdit(profile)}>
              <FormattedMessage
                defaultMessage="Update policy"
                description="Label for the update profile policy button"
              />
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onRemove(profile)}>
              <FormattedMessage
                defaultMessage="Delete profile"
                description="Label for the delete profile policy button"
              />
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </ItemActions>
    </Item>
  );
}

function SkeletonLoader () {
  return (
    <article data-slot="configuration-profiles-skeleton" className="flex justify-between p-4 gap-4">
      <div className="space-y-3">
        <Skeleton className="w-48 h-4"/>
        <Skeleton className="w-48 h-4"/>
      </div>
      <div className="space-y-3">
        <Skeleton className="w-48 h-4"/>
      </div>
    </article>
  );
}

function ChangeProfileName ({ namespace, service, profile, onError }: {
  namespace: Namespace,
  service: Service,
  profile: Profile,
  onError: (error: unknown) => void,
}) {
  const { mutateAsync: onRenameProfile } = useUpdateProfile(namespace, service, profile);

  const onRename = useCallback(async (value?: string) => {
    await onRenameProfile({ id: profile.id, name: value });
  }, [profile]);

  return (
    <div className="flex items-center gap-2 min-w-0">
      <InlineEdit
        value={profile.name}
        onChange={onRename}
        onError={onError}
      >
        <InlineEditPlaceholder
          render={
            <button
              type="button"
              className="flex items-center gap-1.5 text-foreground hover:text-foreground/80 transition-colors group/name truncate max-w-50"
            >
              {profile.name}
              <PencilIcon
                className="size-2.5 text-muted-foreground/50 opacity-0 group-hover/name:opacity-100 transition-opacity shrink-0"/>
            </button>
          }
        />
        <InlineEditInput
          placeholder="Profile name..."
          className="h-6 text-xs w-48"
        />
      </InlineEdit>
    </div>
  );
}

function ChangeProfileDescription ({ namespace, service, profile, onError }: {
  namespace: Namespace,
  service: Service,
  profile: Profile,
  onError: (error: unknown) => void,
}) {
  const { mutateAsync: onRenameProfile } = useUpdateProfile(namespace, service, profile);

  const onRename = useCallback(async (value?: string) => {
    await onRenameProfile({ id: profile.id, description: value });
  }, [profile]);

  return (
    <div className="flex items-center gap-2 min-w-0">
      <InlineEdit
        value={profile.description || ''}
        onChange={onRename}
        onError={onError}
      >
        <InlineEditPlaceholder
          render={
            <button
              type="button"
              className="flex items-center gap-1.5 text-foreground hover:text-foreground/80 transition-colors group/name truncate max-w-50"
            >
              {profile.description}
              <PencilIcon
                className="size-2.5 text-muted-foreground/50 opacity-0 group-hover/name:opacity-100 transition-opacity shrink-0"/>
            </button>
          }
        />
        <InlineEditInput
          placeholder="Profile description..."
          className="h-6 text-xs w-48"
        />
      </InlineEdit>
    </div>
  );
}