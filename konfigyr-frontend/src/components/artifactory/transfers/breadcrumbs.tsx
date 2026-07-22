import { Link } from '@tanstack/react-router';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';

import { TransfersLabel } from '@konfigyr/components/artifactory/transfers/messages';
import type { ReactNode } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export function TransfersBreadcrumbs({ namespace, children }: { namespace: Namespace; children: ReactNode }) {
  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <Link
                to="/namespace/$namespace/artifactory/transfers"
                params={{ namespace: namespace.slug }}
              >
                <TransfersLabel />
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
