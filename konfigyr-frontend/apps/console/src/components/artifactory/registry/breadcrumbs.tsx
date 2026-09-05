import { Link } from '@tanstack/react-router';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';

import { RegistryLabel } from '@konfigyr/components/artifactory/registry/messages';
import type { ReactNode } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export function RegistryBreadcrumbs({ namespace, children }: { namespace: Namespace; children: ReactNode }) {
  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <Link
                to="/namespace/$namespace/artifactory/registry"
                params={{ namespace: namespace.slug }}
              >
                <RegistryLabel />
              </Link>
            }
          />
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        {children}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
