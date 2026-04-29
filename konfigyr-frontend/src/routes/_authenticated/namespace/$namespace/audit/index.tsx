import { z } from 'zod';
import { useCallback } from 'react';
import { useNamespace } from '@konfigyr/hooks';
import { createFileRoute } from '@tanstack/react-router';
import { LayoutContent, LayoutNavbar } from '@konfigyr/components/layout';
import { AuditRecordList } from '@konfigyr/components/audit/audit-record-list';

import type { AuditRecordQuery } from '@konfigyr/hooks/types';

const searchQuerySchema = z.object({
  entityType: z.string().optional().catch(undefined),
  from: z.string().optional().catch(undefined),
  to: z.string().optional().catch(undefined),
  sort: z.string().optional().catch(undefined),
});

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/audit/',
)({
  validateSearch: searchQuerySchema,
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const query: AuditRecordQuery = Route.useSearch();
  const namespace = useNamespace();

  const onQueryChange = useCallback(async (value: AuditRecordQuery) => {
    await navigate({
      search: current => ({ ...current, ...value }),
      viewTransition: false,
    });
  }, []);

  return (
    <LayoutContent>
      <LayoutNavbar title="Audit logs" />

      <div className="w-full space-y-6 px-4">
        <AuditRecordList
          namespace={namespace}
          query={query}
          onQueryChange={onQueryChange}
        />
      </div>
    </LayoutContent>
  );
}
