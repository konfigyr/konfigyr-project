import { FormattedMessage } from 'react-intl';
import { PackageIcon } from 'lucide-react';
import { useIsNamespaceAdmin } from '@konfigyr/hooks';
import { Card, CardContent, CardFooter, CardHeader } from '@konfigyr/components/ui/card';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';
import { ChangeVisibilityButton } from '@konfigyr/components/artifactory/registry/change-visibility-button';
import { RepositoryLabel, WebsiteLabel } from '@konfigyr/components/artifactory/registry/messages';

import type { ArtifactDefinition } from '@konfigyr/hooks/artifactory/types';
import type { Namespace } from '@konfigyr/hooks/types';

export function ArtifactOverview({ namespace, artifact }: { namespace: Namespace; artifact: ArtifactDefinition }) {
  const isAdmin = useIsNamespaceAdmin(namespace);

  return (
    <>
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-card">
            <PackageIcon className="size-5 text-muted-foreground" aria-hidden="true"/>
          </div>
          <div className="min-w-0">
            <h1 className="truncate font-mono text-2xl font-medium leading-tight">
              {artifact.groupId}:{artifact.artifactId}
            </h1>
            {artifact.name && (
              <p className="text-sm text-muted-foreground truncate">{artifact.name}</p>
            )}
          </div>
        </div>
        <ArtifactVisibilityBadge
          size="lg"
          visibility={artifact.visibility}
        />
      </div>

      <Card className="border">
        <CardHeader title="Details"/>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4 text-sm">
            {artifact.description && (
              <div className="col-span-2">
                <dt className="text-muted-foreground">Description</dt>
                <dd>{artifact.description}</dd>
              </div>
            )}
            <div>
              <dt className="text-muted-foreground"><WebsiteLabel/></dt>
              <dd>
                {artifact.website ? (
                  <a href={artifact.website} target="_blank" rel="noreferrer" className="underline text-primary">
                    {artifact.website}
                  </a>
                ) : (
                  <span className="text-muted-foreground">&mdash;</span>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground"><RepositoryLabel/></dt>
              <dd>
                {artifact.repository ? (
                  <a href={artifact.repository} target="_blank" rel="noreferrer" className="underline text-primary">
                    {artifact.repository}
                  </a>
                ) : (
                  <span className="text-muted-foreground">&mdash;</span>
                )}
              </dd>
            </div>
          </dl>
        </CardContent>
        <CardFooter className="justify-between">
          {artifact.visibility === 'PUBLIC' && (
            <FormattedMessage
              defaultMessage="Visible to every namespace."
              description="Label used to describe the public artifact visibility."
              tagName="p"
            />
          )}

          {artifact.visibility === 'PRIVATE' && (
            <FormattedMessage
              defaultMessage="Visible to everyone in the {namespace} namespace."
              description="Label used to describe the private artifact visibility."
              values={{ namespace: <strong>{namespace.slug}</strong> }}
              tagName="p"
            />
          )}

          {isAdmin && (
            <ChangeVisibilityButton namespace={namespace.slug} artifact={artifact}/>
          )}
        </CardFooter>
      </Card>
    </>
  );
}
