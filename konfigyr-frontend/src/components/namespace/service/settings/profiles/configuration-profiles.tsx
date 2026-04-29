import { Item, ItemActions, ItemContent, ItemDescription, ItemGroup, ItemTitle } from '@konfigyr/components/ui/item';
import { FormattedMessage } from 'react-intl';
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
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@konfigyr/components/ui/dropdown-menu';
import { CheckIcon, ChevronDownIcon, PencilIcon, TrashIcon } from 'lucide-react';
import {
  DeleteConfigurationProfileAlert,
} from '@konfigyr/components/namespace/service/settings/profiles/delete-profile-alert';
import { InlineEdit, InlineEditInput, InlineEditPlaceholder } from '@konfigyr/components/ui/inline-edit';
import { toast } from 'sonner';
import { usePolicyDescription, usePolicyLabel } from '@konfigyr/components/vault/profile/policy-picker';
import { ProfilePolicyLabel } from '@konfigyr/components/vault/profile/messages';
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
                      <Link to="/namespace/$namespace/services/$service/create-profile" params={{ namespace: namespace.slug, service: service.slug }}>
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

  const onPolicyUpdate = useCallback(async (value: ProfilePolicy) => {
    try {
      await updateProfile({ id: profile.id, policy: value });
      toast.success(<FormattedMessage
        defaultMessage="The profile policy was successfully updated."
        description="Success message for the profile policy update"
      />);
    } catch (error) {
      return errorNotification(error);
    }
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
        <ItemTitle>
          <ProfileInlineEdit field={profile.name} onChange={onNameUpdate} onError={errorNotification} />
        </ItemTitle>
        <ItemDescription>
          <ProfileInlineEdit field={profile.description} onChange={onDescriptionUpdate} onError={errorNotification} />
        </ItemDescription>
      </ItemContent>

      <div>
        <DropdownMenu>
          <DropdownMenuTrigger render={<Button variant="outline"> <ProfilePolicyLabel value={profile.policy}/> <ChevronDownIcon /></Button>} />
          <DropdownMenuContent className="w-xl" align="end" >
            <DropdownMenuGroup>
              {POLICIES.map((p) => (
                <DropdownMenuItem key={p} >
                  <CheckIcon className={p === profile.policy ? '' : 'invisible'} />
                  <Item className="w-full p-2">
                    <ItemContent>
                      <PolicyItem policy={p} onClick={onPolicyUpdate} />
                    </ItemContent>
                  </Item>
                </DropdownMenuItem>
              ))}
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <ItemActions>
        <Button variant="destructive" onClick={() => onRemove(profile)}>
          <TrashIcon />
        </Button>
      </ItemActions>
    </Item>
  );
}

function PolicyItem({ policy, onClick }: { policy: ProfilePolicy, onClick: (policy: ProfilePolicy) => void, }) {
  const label = usePolicyLabel(policy);
  const description = usePolicyDescription(policy);
  return (
    <ItemContent onClick={() => onClick(policy)}>
      <p>{label}</p>
      <p>{description}</p>
    </ItemContent>
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

function ProfileInlineEdit ({ field, onChange, onError }: {
  field?: string,
  onChange: (value?: string) => Promise<string | number | undefined>,
  onError: (error: unknown) => void,
}) {
  return (
    <span className="flex items-center gap-2 min-w-0">
      <InlineEdit value={field || ''} onChange={onChange} onError={onError} >
        <InlineEditPlaceholder
          render={
            <button
              type="button"
              className="flex items-center gap-1.5 text-foreground hover:text-foreground/80 transition-colors group/name truncate max-w-50"
            >
              {field}
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
    </span>
  );
}