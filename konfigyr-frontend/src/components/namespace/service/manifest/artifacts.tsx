import { FormattedMessage } from 'react-intl';
import { GroupIcon } from 'lucide-react';
import { useServiceManifestQuery } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Card,
  CardContent,
} from '@konfigyr/components/ui/card';
import {
  Item,
  ItemContent,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import {
  MissingManifestsDescription,
  ServiceManifestsInstructions,
} from '../messages';

import type { Artifact, Namespace, Service } from '@konfigyr/hooks/types';

function SkeletonLoader() {
  return (
    <article data-slot="artifact-skeleton" className="flex justify-between items-center p-4 gap-4">
      <div className="grow space-y-3">
        <Skeleton className="w-48 h-4" />
        <Skeleton className="w-64 h-4" />
      </div>
      <Skeleton className="w-2 h-4 mr-2" />
    </article>
  );
}

function ArtifactItem({ artifact }: { artifact: Artifact }) {
  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle className="font-mono">
          <span>{artifact.groupId}:{artifact.artifactId}</span>
          <Badge variant="outline">{artifact.version}</Badge>
        </ItemTitle>
      </ItemContent>
    </Item>
  );
}

export function ServiceArtifacts({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { data: manifest, error, isPending, isError } = useServiceManifestQuery(namespace.slug, service.slug);

  return (
    <Card className="border">
      <CardContent>
        {isPending && (
          <SkeletonLoader />
        )}

        {isError && (
          <ErrorState error={error} className="border-none" />
        )}

        {manifest?.artifacts.length === 0 && (
          <EmptyState
            icon={<GroupIcon size="2rem" />}
            title={
              <FormattedMessage
                defaultMessage="No artifacts found"
                description="Empty state title used when no artifacts are present in the service manifest."
              />
            }
            description={
              <MissingManifestsDescription />
            }
          >
            <p className="text-muted-foreground text-sm/relaxed">
              <ServiceManifestsInstructions />
            </p>
          </EmptyState>
        )}

        {manifest?.artifacts && (
          <ItemGroup>
            {manifest.artifacts.map(artifact => (
              <ArtifactItem
                key={`${artifact.groupId}:${artifact.artifactId}:${artifact.version}`}
                artifact={artifact}
              />
            ))}
          </ItemGroup>
        )}
      </CardContent>
    </Card>
  );
}
