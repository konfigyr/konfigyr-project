import { z } from 'zod';
import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { useNamespace } from '@konfigyr/hooks';
import { Invitations } from '@konfigyr/components/namespace/members/invitations';
import { createFileRoute } from '@tanstack/react-router';

import type { Pageable } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  page: z.number().optional().catch(undefined),
  size: z.number().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/invitations',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const pageable: Pageable = Route.useSearch();

  return (
    <LayoutContent>
      <LayoutNavbar title="Invitations"/>
      <div className="w-full space-y-6 px-4">
        <Invitations
          namespace={namespace}
          pageable={pageable}
        />
      </div>
    </LayoutContent>
  );
}
