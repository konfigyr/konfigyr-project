import { TagIcon } from 'lucide-react';
import { FormattedDate } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { ActionsLabel, ViewLabel } from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import { buttonVariants } from '@konfigyr/components/ui/button';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { PageResponsePagination } from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@konfigyr/components/ui/table';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';
import {
  NoVersionsFoundTitle,
  PublishedAtLabel,
  VisibilityLabel,
} from '@konfigyr/components/artifactory/registry/messages';

import type { VersionedArtifact } from '@konfigyr/hooks/artifactory/types';
import type { PageResponse } from '@konfigyr/hooks/hateoas/types';

function VersionRow({ namespace, version }: { namespace: string; version: VersionedArtifact }) {
  return (
    <TableRow>
      <TableCell className="font-mono font-bold">
        {version.version}
      </TableCell>
      <TableCell>
        <ArtifactVisibilityBadge visibility={version.visibility}/>
      </TableCell>
      <TableCell>
        <time dateTime={version.publishedAt}>
          <FormattedDate value={version.publishedAt} day="2-digit" month="short" year="numeric"/>
        </time>
      </TableCell>
      <TableCell className="text-right">
        <Link
          to="/namespace/$namespace/artifactory/registry/$groupId/$artifactId/$version"
          params={{ namespace, groupId: version.groupId, artifactId: version.artifactId, version: version.version }}
          className={buttonVariants({ variant: 'ghost' })}
        >
          <ViewLabel/>
        </Link>
      </TableCell>
    </TableRow>
  );
}

function VersionSkeleton() {
  return (
    <TableRow data-slot="version-skeleton">
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-20 rounded-xl"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24 ml-auto"/></TableCell>
    </TableRow>
  );
}

export function VersionTable({ namespace, data, error, isPending, page, size }: {
  namespace: string;
  data?: PageResponse<VersionedArtifact>;
  error?: Error | null;
  isPending?: boolean;
  page?: number;
  size?: number;
}) {
  return (
    <>
      <Card className="border">
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-32">
                  Version
                </TableHead>
                <TableHead className="w-32">
                  <VisibilityLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <PublishedAtLabel/>
                </TableHead>
                <TableHead className="w-48 text-right">
                  <ActionsLabel/>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending && (
                <>
                  <VersionSkeleton/>
                  <VersionSkeleton/>
                </>
              )}

              {data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>
                    <EmptyState
                      icon={<TagIcon size="2rem"/>}
                      title={<NoVersionsFoundTitle/>}
                    />
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((version) => (
                <VersionRow key={version.id} namespace={namespace} version={version}/>
              ))}
            </TableBody>
          </Table>

          {error && <ErrorState error={error}/>}
        </CardContent>
      </Card>

      <PageResponsePagination page={page} size={size} response={data} className="mt-4"/>
    </>
  );
}
