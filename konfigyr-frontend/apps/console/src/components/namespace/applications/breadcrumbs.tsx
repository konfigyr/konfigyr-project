import { Link } from '@tanstack/react-router';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';
import { NamespaceApplicationTitle } from './messages';

import type { ReactNode } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export function ApplicationsBreadcrumbs({ namespace, children }: { namespace: Namespace, children: ReactNode }) {
  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink
            render={
              <Link
                to="/namespace/$namespace/applications"
                params={{ namespace: namespace.slug }}
              >
                <NamespaceApplicationTitle />
              </Link>
            }
          />
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        <BreadcrumbItem>
          <BreadcrumbPage>
            {children}
          </BreadcrumbPage>
        </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
  );
}
