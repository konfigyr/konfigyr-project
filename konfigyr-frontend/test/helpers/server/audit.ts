import { HttpResponse, http } from 'msw';
import * as namespaces from '../mocks/namespace';
import { allRecords } from '../mocks/audit';

import type { AuditRecord, CursorResponse } from '@konfigyr/hooks/types';

const search = http.get('http://localhost/api/namespaces/:slug/audit', ({ params, request }) => {
  if (params.slug === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace member with slug '${params.slug}' not found.`,
    }, { status: 404 });
  }

  if (params.slug === namespaces.johnDoe.slug) {
    return HttpResponse.json({ data: [] });
  }

  const url = new URL(request.url);
  const entityType = url.searchParams.get('entityType');
  const entityId = url.searchParams.get('entityId');

  let data = allRecords;

  if (entityType) {
    data = data.filter(record => record.entityType === entityType);
  }

  if (entityId) {
    data = data.filter(record => record.entityId === entityId);
  }

  const response: CursorResponse<AuditRecord> = {
    data,
    metadata: {
      size: 20,
      ...(data.length === allRecords.length ? { next: 'next-token' } : {}),
    },
  };

  return HttpResponse.json(response);
});

export default [
  search,
];
