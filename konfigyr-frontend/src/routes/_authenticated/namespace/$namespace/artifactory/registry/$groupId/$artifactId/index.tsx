import { FormattedMessage } from 'react-intl';
import { PackageIcon } from 'lucide-react';
import { createFileRoute } from '@tanstack/react-router';
import { useGetArtifact, useGetArtifactVersions, useNamespace } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { RegistryBreadcrumbs } from '@konfigyr/components/artifactory/registry/breadcrumbs';
import { ArtifactOverview } from '@konfigyr/components/artifactory/registry/artifact-overview';
import { VersionTable } from '@konfigyr/components/artifactory/registry/version-table';
import { VersionsLabel } from '@konfigyr/components/artifactory/registry/messages';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/registry/$groupId/$artifactId/',
)({
  component: RouteComponent,
});

function RouteComponent () {
  const namespace = useNamespace();
  const { groupId, artifactId } = Route.useParams();

  const {
    data: artifact,
    error: artifactError,
    isError: isArtifactError,
    isPending: isArtifactPending,
  } = useGetArtifact(namespace.slug, groupId, artifactId);

  const {
    data: versions,
    error: versionsError,
    isPending: isVersionsPending,
  } = useGetArtifactVersions(namespace.slug, groupId, artifactId);

  return (
    <>
      <RegistryBreadcrumbs namespace={namespace}>
        {groupId}:{artifactId}
      </RegistryBreadcrumbs>

      <div className="mx-auto w-full space-y-6 lg:w-2/3 xl:w-3/5">
        {isArtifactPending && (
          <EmptyState
            icon={<PackageIcon/>}
            title={(
              <FormattedMessage
                defaultMessage="Loading artifact"
                description="Loading state title for the artifact overview page."
              />
            )}
            description={(
              <FormattedMessage
                defaultMessage="Fetching the current artifact metadata."
                description="Loading state description for the artifact overview page."
              />
            )}
          />
        )}

        {isArtifactError && (
          <ErrorState error={artifactError} className="border-none"/>
        )}

        {artifact && (
          <>
            <ArtifactOverview namespace={namespace} artifact={artifact}/>

            <h2 className="text-lg font-medium">
              <VersionsLabel/>
            </h2>

            <VersionTable
              namespace={namespace.slug}
              data={versions}
              error={versionsError}
              isPending={isVersionsPending}
            />
          </>
        )}
      </div>
    </>
  );
}
