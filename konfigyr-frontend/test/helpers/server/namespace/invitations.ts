import { HttpResponse, http } from 'msw';
import { parseISO } from 'date-fns';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
import { accounts, namespaces } from '../../mocks';

import type { Invitation, PageResponse } from '@konfigyr/hooks/types';

const list = http.get('http://localhost/api/namespaces/:slug/invitations', ({ params }) => {
  if (params.slug === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace member with slug '${params.slug}' not found.`,
    }, { status: 404 });
  }

  if (params.slug === namespaces.johnDoe.slug) {
    return HttpResponse.json({ data: [], metadata: {} });
  }

  const response: PageResponse<Invitation> = {
    data: [{
      key: '0b9f514567f6cd9bb393a06388fc3dd7',
      sender: {
        id: accounts.johnDoe.id,
        email: accounts.johnDoe.email,
        name: accounts.johnDoe.fullName,
      },
      recipient: {
        email: 'recipient-user@konfigyr.com',
      },
      role: NamespaceRole.USER,
      expired: false,
      createdAt: parseISO('2026-04-17T09:45:12'),
      expiryDate: parseISO('2026-04-28T19:45:16'),
    }, {
      key: '0b9f532868b6cdd793f703d7d09eb301',
      sender: {
        id: accounts.johnDoe.id,
        email: accounts.johnDoe.email,
        name: accounts.johnDoe.fullName,
      },
      recipient: {
        email: 'recipient-admin@konfigyr.com',
      },
      role: NamespaceRole.ADMIN,
      expired: false,
      createdAt: parseISO('2026-04-11T11:30:30'),
      expiryDate: parseISO('2026-04-17T16:30:30'),
    }, {
      key: '9456532868b6cdd235703d7d09eb301',
      sender: {
        id: accounts.janeDoe.id,
        email: accounts.janeDoe.email,
        name: accounts.janeDoe.fullName,
      },
      recipient: {
        email: 'expired@konfigyr.com',
      },
      role: NamespaceRole.ADMIN,
      expired: true,
      createdAt: parseISO('2026-04-11T17:30:30'),
      expiryDate: parseISO('2026-04-17T17:30:30'),
    }],
    metadata: {
      number: 2,
      size: 3,
      total: 9,
      pages: 3,
    },
  };

  return HttpResponse.json(response);
});

export default [
  list,
];
