import { BoxesIcon, ListTreeIcon } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import { TabItem, Tabs } from '@konfigyr/components/ui/tab';
import { ArtifactsLabel, ConfigurationPropertiesLabel } from '../messages';

import type { Namespace, Service } from '@konfigyr/hooks/types';

export function ManifestMenu({ namespace, service }: { namespace: Namespace, service: Service }) {
  return (
    <Tabs>
      <TabItem
        render={
          <Link
            to="/namespace/$namespace/services/$service/manifest"
            params={{ namespace: namespace.slug, service: service.slug }}
            activeProps={{ 'data-state': 'active' }}
            activeOptions={{ exact: true }}
          >
            <ListTreeIcon />
            <ConfigurationPropertiesLabel />
          </Link>
        }
      />
      <TabItem
        render={
          <Link
            to="/namespace/$namespace/services/$service/manifest/artifacts"
            params={{ namespace: namespace.slug, service: service.slug }}
            activeProps={{ 'data-state': 'active' }}
            activeOptions={{ exact: true }}
          >
            <BoxesIcon />
            <ArtifactsLabel />
          </Link>
        }
      />
    </Tabs>
  );
}
