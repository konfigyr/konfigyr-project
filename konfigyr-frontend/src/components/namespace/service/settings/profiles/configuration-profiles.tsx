import { Item, ItemActions, ItemContent, ItemDescription, ItemGroup, ItemTitle } from '@konfigyr/components/ui/item';
import { FormattedMessage, useIntl } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@konfigyr/components/ui/card';
import { ErrorState, useErrorNotification } from '@konfigyr/components/error';
import { useGetProfiles, useUpdateProfile } from '@konfigyr/hooks';
import React, { useCallback, useState } from 'react';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Link } from '@tanstack/react-router';
import { PolicyAlertIcon } from '@konfigyr/components/vault/profile/policy-alert';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { ChevronDownIcon, PencilIcon, TrashIcon } from 'lucide-react';
import {
  DeleteConfigurationProfileAlert,
} from '@konfigyr/components/namespace/service/settings/profiles/delete-profile-alert';
import { InlineEdit, InlineEditInput, InlineEditPlaceholder } from '@konfigyr/components/ui/inline-edit';
import { toast } from 'sonner';
import { usePolicyDescription, usePolicyLabel } from '@konfigyr/components/vault/profile/policy-picker';
import { ProfilePolicyLabel } from '@konfigyr/components/vault/profile/messages';

import type { ReactNode } from 'react';
import type { Profile, ProfilePolicy } from '@konfigyr/hooks/vault/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

export type ProfileItemProps = {
  namespace: Namespace,
  service: Service,
  profile: Profile,
  onRemove: (profile: Profile) => void
};

const POLICIES: Array<ProfilePolicy> = ['PROTECTED', 'UNPROTECTED', 'IMMUTABLE'];

export function ServiceConfigurationProfiles ({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { data: profiles, error, isPending, isError } = useGetProfiles(namespace, service);

  const [removing, setRemoving] = useState<Profile | undefined>();

  const onCloseRemoveProfileAlert = useCallback(() => setRemoving(undefined), []);

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
                      <Link to="/namespace/$namespace/services/$service/create-profile"
                        params={{ namespace: namespace.slug, service: service.slug }}>
                        {chunks}
                      </Link>
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
    </>
  );
}

export function ProfileItem ({ namespace, service, profile, onRemove }: ProfileItemProps) {
  const { mutateAsync: updateProfile } = useUpdateProfile(namespace, service, profile);
  const errorNotification = useErrorNotification();
  const [open, setOpen] = useState(false);
  const intl = useIntl();

  const onPolicyUpdate = useCallback(async (value: ProfilePolicy) => {
    try {
      await updateProfile({ id: profile.id, policy: value });
    } catch (error) {
      return errorNotification(error);
    } finally {
      setOpen(false);
    }

    return toast.success(intl.formatMessage({
      defaultMessage: 'The profile policy was successfully updated.',
      description: 'Success message for the profile policy update',
    }));
  }, [profile]);

  const onNameUpdate = useCallback(async (value?: string) => {
    try {
      await updateProfile({ id: profile.id, name: value });
    } catch (error) {
      return errorNotification(error);
    }
  }, [profile]);

  const onDescriptionUpdate = useCallback(async (value?: string) => {
    try {
      await updateProfile({ id: profile.id, description: value });
    } catch (error) {
      return errorNotification(error);
    }
  }, [profile]);

  return (
    <Item variant="list">
      <div>
        <PolicyAlertIcon policy={profile.policy}/>
      </div>
      <ItemContent>
        <ItemTitle className="overflow-visible">
          <InlineEdit value={profile.name} onChange={onNameUpdate} onError={errorNotification}>
            <ProfileInlineEditPlaceholder>
              {profile.name}
            </ProfileInlineEditPlaceholder>
            <InlineEditInput className="h-6 text-xs" />
          </InlineEdit>
        </ItemTitle>
        <ItemDescription className="overflow-visible">
          <InlineEdit value={profile.description || ''} onChange={onDescriptionUpdate} onError={errorNotification}>
            <ProfileInlineEditPlaceholder>
              {profile.description ? profile.description : (
                <FormattedMessage
                  tagName="i"
                  defaultMessage="No description provided"
                  description="Default description for the profile"
                />
              )}
            </ProfileInlineEditPlaceholder>
            <InlineEditInput className="h-6 text-xs" />
          </InlineEdit>
        </ItemDescription>
      </ItemContent>

      <div>
        <DropdownMenu open={open} onOpenChange={setOpen}>
          <DropdownMenuTrigger render={
            <Button
              variant="outline"
              onClick={() => setOpen(true)}
              aria-label={intl.formatMessage({
                defaultMessage: 'Update policy for {name} profile',
                description: 'Label for the update profile policy button. This message accepts the profile name as a value.',
              }, {
                name: profile.name,
              })}
            >
              <ProfilePolicyLabel value={profile.policy}/>
              <ChevronDownIcon/>
            </Button>
          }/>
          <DropdownMenuContent className="w-xl" align="end">
            <DropdownMenuGroup>
              <DropdownMenuRadioGroup value={profile.policy} onValueChange={onPolicyUpdate}>
                {POLICIES.map((p) => (
                  <DropdownMenuRadioItemPolicy key={p} policy={p} />
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <ItemActions>
        <Button
          variant="destructive"
          onClick={() => onRemove(profile)}
          aria-label={intl.formatMessage({
            defaultMessage: 'Delete {name} profile',
            description: 'Label for the delete profile button. This message accepts the profile name as a value.',
          }, {
            name: profile.name,
          })}
        >
          <TrashIcon/>
        </Button>
      </ItemActions>
    </Item>
  );
}

function DropdownMenuRadioItemPolicy ({ policy }: { policy: ProfilePolicy }) {
  const label = usePolicyLabel(policy);
  const description = usePolicyDescription(policy);
  return (
    <DropdownMenuRadioItem value={policy} aria-label={label}>
      <div className="w-full p-1 text-sm">
        <p className="font-medium leading-relaxed">{label}</p>
        <p className="text-muted-foreground">{description}</p>
      </div>
    </DropdownMenuRadioItem>
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

function ProfileInlineEditPlaceholder({ children }: { children: ReactNode }) {
  return (
    <InlineEditPlaceholder
      render={
        <button
          type="button"
          className="flex items-center gap-1.5 text-foreground hover:text-foreground/80 transition-colors group/placeholder truncate max-w-50"
        >
          {children}
          <PencilIcon
            className="size-2.5 text-muted-foreground/50 opacity-0 group-hover/placeholder:opacity-100 transition-opacity shrink-0"/>
        </button>
      }
    />
  );
}
