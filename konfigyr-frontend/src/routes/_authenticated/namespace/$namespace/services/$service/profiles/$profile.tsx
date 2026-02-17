import { FormattedMessage } from 'react-intl';
import { ProfileMenu } from '@konfigyr/components/vault/navigation/profile-menu';
import { ChangesetEditor } from '@konfigyr/components/vault/changeset/editor';
import { PolicyAlert } from '@konfigyr/components/vault/profile/policy-alert';
import { getProfileQuery, getProfilesQuery, useChangesetState } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';

import type { Namespace, Service } from '@konfigyr/hooks/types';

const generatePageTitle = (namespace?: string, service?: string, profile?: string) => {
  if (profile && namespace && service) {
    return `${profile} | ${service} | ${namespace} | Konfigyr`;
  }
  return 'Konfigyr';
};

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/profiles/$profile',
)({
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };

    const profiles = await context.queryClient.ensureQueryData(getProfilesQuery(namespace, service));
    const profile = await context.queryClient.ensureQueryData(getProfileQuery(namespace, service, params.profile));

    return { namespace, service, profiles, profile };
  },
  head: ({ loaderData }) => ({
    meta: [{
      title: generatePageTitle(
        loaderData?.namespace.name,
        loaderData?.service.name,
        loaderData?.profile.name,
      ),
    }],
  }),
  component: RouteComponent,
});

function RouteComponent() {
  const { namespace, service, profiles, profile } = Route.useLoaderData();
  const { data } = useChangesetState(profile);

  return (
    <div className="mx-4 space-y-6">
      <header>
        <p className="font-medium text-xl/relaxed">
          <FormattedMessage
            defaultMessage="Configuration overview"
            description="Title of the configuration overview page"
          />
        </p>
        <p className="text-muted-foreground text-sm/relaxed">
          <FormattedMessage
            defaultMessage="Manage and version configuration values per profile."
            description="Subtitle of the configuration overview page"
          />
        </p>
      </header>

      <aside className="">
        <ProfileMenu
          namespace={namespace}
          profiles={profiles}
          service={service}
        />
      </aside>

      <PolicyAlert profile={profile} />

      <div>
        {data && (
          <ChangesetEditor changeset={data} />
        )}
      </div>
    </div>
  );
}
