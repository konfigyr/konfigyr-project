import { Link } from '@tanstack/react-router';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';

import { GroupClaimsLabel } from '@konfigyr/components/groups/messages';
import type { ReactNode } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export function GroupsBreadcrumbs({ namespace, children }: { namespace: Namespace; children: ReactNode }) {
  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <Link
                to="/namespace/$namespace/groups"
                params={{ namespace: namespace.slug }}
              >
                <GroupClaimsLabel />
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
