import { HttpResponse, http } from 'msw';
import { janeDoe, johnDoe } from '../mocks/account';
import * as namespaces from '../mocks/namespace';

import type { AuditRecord, CursorResponse } from '@konfigyr/hooks/types';

const search = http.get('http://localhost/api/namespaces/:slug/audit', ({ params }) => {
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

  const response: CursorResponse<AuditRecord> = {
    data: [{
      id: 'fourth-audit-record',
      entityType: 'keyset',
      entityId: 'kms-keyset',
      eventType: 'keyset.created',
      message: 'Keyset was created',
      actor: {
        id: janeDoe.id,
        type: 'USER_ACCOUNT',
        name: janeDoe.fullName ?? janeDoe.email,
      },
      createdAt: '2026-04-28T20:51:56.341973+02:00',
    }, {
      id: 'third-audit-record',
      entityType: 'profile',
      entityId: 'development',
      eventType: 'profile.created',
      message: 'Profile was created',
      actor: {
        id: johnDoe.id,
        type: 'USER_ACCOUNT',
        name: johnDoe.fullName ?? johnDoe.email,
      },
      createdAt: '2026-04-28T20:51:56.341973+02:00',
    }, {
      id: 'second-audit-record',
      entityType: 'service',
      entityId: 'konfgyr-api',
      eventType: 'service.created',
      message: 'Service was created',
      actor: {
        id: johnDoe.id,
        type: 'USER_ACCOUNT',
        name: johnDoe.fullName ?? johnDoe.email,
      },
      createdAt: '2026-04-28T20:51:56.341973+02:00',
    }, {
      id: 'first-audit-record',
      entityType: 'namespace',
      entityId: namespaces.konfigyr.id,
      eventType: 'namespace.created',
      message: 'Namespace was created',
      actor: {
        id: johnDoe.id,
        type: 'USER_ACCOUNT',
        name: johnDoe.fullName ?? johnDoe.email,
      },
      createdAt: '2026-04-28T20:51:39.399083+02:00',
    }],
    metadata: {
      size: 20,
      next: 'next-token',
    },
  };

  return HttpResponse.json(response);
});

export default [
  search,
];
