import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import { ProfileMenu } from '@konfigyr/components/vault/navigation/profile-menu';
import { ChangesetEditor } from '@konfigyr/components/vault/changeset/editor';
import { PolicyAlert } from '@konfigyr/components/vault/profile/policy-alert';
import {
  getProfileQuery,
  getProfilesQuery,
  useChangesetState,
  useServiceCatalogQuery,
} from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { ChangeHistoryAlert } from '@konfigyr/components/vault/change-history/change-history-alert';
import { ProgressLoader } from '@konfigyr/components/ui/loader';
import type { ChangeRequest, Namespace, Service } from '@konfigyr/hooks/types';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/profiles/$profile/',
)({
  loader: async ({ context, params, parentMatchPromise }) => {
    const match = await parentMatchPromise;
    const { namespace, service } = match.loaderData as { namespace: Namespace, service: Service };

    const profiles = await context.queryClient.ensureQueryData(getProfilesQuery(namespace, service));
    const profile = await context.queryClient.ensureQueryData(getProfileQuery(namespace, service, params.profile));

    return { namespace, service, profiles, profile };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const { namespace, service, profiles, profile } = Route.useLoaderData();
  const { data: changeset, isLoading: isChangesetLoading } = useChangesetState(namespace, service, profile);
  const { data: catalog, isLoading: isCatalogLoading } = useServiceCatalogQuery(namespace.slug, service.slug);

  const onChangeRequestCreated = useCallback((changeRequest: ChangeRequest) => {
    return navigate({
      to: '/namespace/$namespace/services/$service/requests/$number',
      params: {
        namespace: namespace.slug,
        service: service.slug,
        number: String(changeRequest.number),
      },
    });
  }, [navigate, namespace, service]);

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

      <ChangeHistoryAlert
        namespace={namespace}
        service={service}
        profile={profile}
      />

      {(isChangesetLoading || isCatalogLoading) && (
        <div className="md:w-1/2 mx-auto px-4 py-8">
          <div className="text-center space-y-1">
            <p className="font-medium">
              <FormattedMessage
                defaultMessage="Loading your profile from the vault..."
                description="Message shown while the profile configuration is being retrieved from the Konfigyr Vault"
              />
            </p>

            <ProgressLoader className="mx-auto my-4" />

            <p className="text-muted-foreground text-sm">
              <FormattedMessage
                defaultMessage="This may take only a moment, please be patient."
                description="Description of the vault loading process"
              />
            </p>
          </div>
        </div>
      )}

      {(changeset && catalog) && (
        <ChangesetEditor
          catalog={catalog}
          changeset={changeset}
          onChangeRequestCreated={onChangeRequestCreated}
        />
      )}
    </div>
  );
}
