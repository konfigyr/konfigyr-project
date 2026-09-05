import { z } from 'zod';
import { TagIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Link, createFileRoute } from '@tanstack/react-router';
import { useGetArtifactVersion, useNamespace } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { BreadcrumbItem, BreadcrumbLink, BreadcrumbSeparator } from '@konfigyr/components/ui/breadcrumb';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { RegistryBreadcrumbs } from '@konfigyr/components/artifactory/registry/breadcrumbs';
import { VersionDetail } from '@konfigyr/components/artifactory/registry/version-detail';

const searchQuerySchema = z.object({
  term: z.string().min(2).optional().catch(undefined),
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/registry/$groupId/$artifactId/$version/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent () {
  const namespace = useNamespace();
  const navigate = Route.useNavigate();
  const { term, page, size } = Route.useSearch();
  const { groupId, artifactId, version } = Route.useParams();

  const { data: artifactVersion, error, isError, isPending } = useGetArtifactVersion(namespace.slug, groupId, artifactId, version);

  return (
    <>
      <RegistryBreadcrumbs namespace={namespace}>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <Link
                to="/namespace/$namespace/artifactory/registry/$groupId/$artifactId"
                params={{ namespace: namespace.slug, groupId, artifactId }}
              >
                {groupId}:{artifactId}
              </Link>
            }
          />
        </BreadcrumbItem>
        <BreadcrumbSeparator/>
        {version}
      </RegistryBreadcrumbs>

      <div className="mx-auto w-full space-y-6 lg:w-2/3 xl:w-3/5">
        {isPending && (
          <EmptyState
            icon={<TagIcon/>}
            title={(
              <FormattedMessage
                defaultMessage="Loading artifact version"
                description="Loading state title for the artifact version detail page."
              />
            )}
            description={(
              <FormattedMessage
                defaultMessage="Fetching the current artifact version metadata."
                description="Loading state description for the artifact version detail page."
              />
            )}
          />
        )}

        {isError && (
          <ErrorState error={error} className="border-none"/>
        )}

        {artifactVersion && (
          <VersionDetail
            namespace={namespace.slug}
            version={artifactVersion}
            term={term}
            page={page}
            size={size}
            onTermChange={value => navigate({ search: current => ({ ...current, term: value, page: 1 }) })}
          />
        )}
      </div>
    </>
  );
}
