import { Link } from '@tanstack/react-router';
import { FormattedMessage } from 'react-intl';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@konfigyr/components/ui/breadcrumb';

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
                <FormattedMessage
                  defaultMessage="Group claims"
                  description="Breadcrumb label for the group verification claims list page."
                />
              </Link>
            }
          />
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        <BreadcrumbItem>
          <BreadcrumbPage>{children}</BreadcrumbPage>
        </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
  );
}
