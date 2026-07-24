import { PackageIcon } from 'lucide-react';
import { FormattedDate } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { ActionsLabel, UpdatedAtLabel, ViewLabel } from '@konfigyr/components/messages';
import { ErrorState } from '@konfigyr/components/error';
import { buttonVariants } from '@konfigyr/components/ui/button';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { PageResponsePagination } from '@konfigyr/components/ui/pagination';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@konfigyr/components/ui/table';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';
import {
  NoArtifactsFoundDescription,
  NoArtifactsFoundTitle,
  VisibilityLabel,
} from '@konfigyr/components/artifactory/registry/messages';

import type { ArtifactDefinition } from '@konfigyr/hooks/artifactory/types';
import type { PageResponse } from '@konfigyr/hooks/hateoas/types';

function ArtifactRow({ namespace, artifact }: { namespace: string; artifact: ArtifactDefinition }) {
  return (
    <TableRow>
      <TableCell className="font-mono font-bold">
        <div className="flex items-center gap-2">
          <PackageIcon className="size-4 text-muted-foreground" aria-hidden="true"/>
          {artifact.groupId}:{artifact.artifactId}
        </div>
      </TableCell>
      <TableCell>
        {artifact.name || <span className="text-muted-foreground">&mdash;</span>}
      </TableCell>
      <TableCell>
        <ArtifactVisibilityBadge visibility={artifact.visibility}/>
      </TableCell>
      <TableCell>
        {artifact.updatedAt ? (
          <time dateTime={artifact.updatedAt}>
            <FormattedDate value={artifact.updatedAt} day="2-digit" month="short" year="numeric"/>
          </time>
        ) : (
          <span className="text-muted-foreground">&mdash;</span>
        )}
      </TableCell>
      <TableCell className="text-right">
        <Link
          to="/namespace/$namespace/artifactory/registry/$groupId/$artifactId"
          params={{ namespace, groupId: artifact.groupId, artifactId: artifact.artifactId }}
          className={buttonVariants({ variant: 'ghost' })}
        >
          <ViewLabel/>
        </Link>
      </TableCell>
    </TableRow>
  );
}

function ArtifactSkeleton() {
  return (
    <TableRow data-slot="artifact-skeleton">
      <TableCell><Skeleton className="h-5 w-48"/></TableCell>
      <TableCell><Skeleton className="h-5 w-32"/></TableCell>
      <TableCell><Skeleton className="h-5 w-20 rounded-xl"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24"/></TableCell>
      <TableCell><Skeleton className="h-5 w-24 ml-auto"/></TableCell>
    </TableRow>
  );
}

export function ArtifactTable({ namespace, data, error, isPending, page, size }: {
  namespace: string;
  data?: PageResponse<ArtifactDefinition>;
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
                <TableHead className="min-w-64">
                  Coordinates
                </TableHead>
                <TableHead className="w-48">
                  Name
                </TableHead>
                <TableHead className="w-32">
                  <VisibilityLabel/>
                </TableHead>
                <TableHead className="w-32">
                  <UpdatedAtLabel/>
                </TableHead>
                <TableHead className="w-48 text-right">
                  <ActionsLabel/>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending && (
                <>
                  <ArtifactSkeleton/>
                  <ArtifactSkeleton/>
                  <ArtifactSkeleton/>
                </>
              )}

              {data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <EmptyState
                      icon={<PackageIcon size="2rem"/>}
                      title={<NoArtifactsFoundTitle/>}
                      description={<NoArtifactsFoundDescription/>}
                    />
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((artifact) => (
                <ArtifactRow key={artifact.id} namespace={namespace} artifact={artifact}/>
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
