import { FormattedMessage } from 'react-intl';
import { BoxesIcon, GroupIcon } from 'lucide-react';
import { useServiceManifestQuery } from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Card,
  CardContent,
  CardHeader,
  CardIcon,
  CardTitle,
} from '@konfigyr/components/ui/card';
import {
  Item,
  ItemContent,
  ItemGroup,
  ItemTitle,
} from '@konfigyr/components/ui/item';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Skeleton } from '@konfigyr/components/ui/skeleton';

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
        <ItemTitle>
          <span>{artifact.groupId}:{artifact.artifactId}</span>
          <Badge variant="outline">{artifact.version}</Badge>
        </ItemTitle>
      </ItemContent>
    </Item>
  );
}

export function ServiceManifest({ namespace, service }: { namespace: Namespace, service: Service }) {
  const { data: manifest, error, isPending, isError } = useServiceManifestQuery(namespace.slug, service.slug);

  return (
    <Card className="border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <CardIcon>
            <BoxesIcon size="1.25rem"/>
          </CardIcon>
          <FormattedMessage
            defaultMessage="Service artifacts"
            description="Title used in the card that lists artifacts from the service manifest."
          />
        </CardTitle>
      </CardHeader>
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
              <FormattedMessage
                defaultMessage="This usually means that the service has not yet published its dependency manifest to Konfigyr."
                description="Empty state description used when no artifacts are present in the service manifest."
              />
            }
          >
            <p className="text-muted-foreground text-sm/relaxed">
              <FormattedMessage
                defaultMessage="To create the manifest, execute a release using the Konfigyr build plugin in the service repository. The plugin will upload the artifact dependency information required to generate the service manifest."
                description="Help text describing how service manifests are created"
              />
            </p>
          </EmptyState>
        )}

        {manifest?.artifacts && (
          <ItemGroup className="-mx-4">
            {manifest.artifacts.map(artifact => (
              <ArtifactItem key={artifact.id} artifact={artifact}/>
            ))}
          </ItemGroup>
        )}
      </CardContent>
    </Card>
  );
}
