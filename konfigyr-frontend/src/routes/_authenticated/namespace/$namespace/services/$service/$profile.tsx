import { LockIcon, ShieldCheckIcon, ShieldOffIcon } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@konfigyr/components/ui/alert';
import { ProfileMenu } from '@konfigyr/components/vault/navigation/profile-menu';
import { ChangesetEditor } from '@konfigyr/components/vault/changeset/editor';
import { useChangesetState } from '@konfigyr/hooks';
import { createFileRoute, notFound } from '@tanstack/react-router';

import type { Profile } from '@konfigyr/hooks/types';

const profiles: Array<Profile> = [
  {
    id: 'development',
    name: 'Development',
    slug: 'development',
    policy: 'UNPROTECTED',
    position: 1,
  },
  {
    id: 'production',
    name: 'Production',
    slug: 'production',
    policy: 'PROTECTED',
    position: 3,
  },
  {
    id: 'staging',
    name: 'Staging',
    slug: 'staging',
    policy: 'PROTECTED',
    position: 2,
  },
  {
    id: 'locked',
    name: 'Locked',
    slug: 'locked',
    policy: 'LOCKED',
    position: 4,
  },
];

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/$profile',
)({
  loader: ({ params }) => {
    const profile = profiles.find(it => it.slug === params.profile);

    if (!profile) {
      throw notFound({ data: `Could not find profile with slug: ${params.profile}` });
    }

    return { profile };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { profile } = Route.useLoaderData();
  const { namespace, service } = Route.parentRoute.useLoaderData();
  const { data } = useChangesetState(profile);

  return (
    <div className="mx-4 space-y-6">
      <header>
        <p className="font-medium text-xl/relaxed">Configuration overview</p>
        <p className="text-muted-foreground text-sm/relaxed">Manage and version configuration values per profile.</p>
      </header>

      <aside className="">
        <ProfileMenu
          namespace={namespace}
          profiles={profiles}
          service={service}
        />
      </aside>

      {profile.policy === 'LOCKED' && (
        <Alert>
          <LockIcon />
          <AlertTitle>
            Profile is locked
          </AlertTitle>
          <AlertDescription>
            This profile is locked and cannot be edited.
          </AlertDescription>
        </Alert>
      )}

      {profile.policy === 'PROTECTED' && (
        <Alert>
          <ShieldCheckIcon />
          <AlertTitle>
            Protected profile
          </AlertTitle>
          <AlertDescription>
            Changes to this profile require review and approval before being applied.
          </AlertDescription>
        </Alert>
      )}

      {profile.policy === 'UNPROTECTED' && (
        <Alert>
          <ShieldOffIcon />
          <AlertTitle>
            Unprotected profile
          </AlertTitle>
          <AlertDescription>
            Changes to this profile will be directly applied without any review or approval.
          </AlertDescription>
        </Alert>
      )}

      <div>
        {data && (
          <ChangesetEditor changeset={data} />
        )}
      </div>
    </div>
  );
}
