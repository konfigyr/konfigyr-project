import { HttpResponse, http } from 'msw';
import { parseISO } from 'date-fns';
import { NamespaceRole } from '@konfigyr/hooks/memberships/types';
import { accounts, namespaces } from '../mocks';

import type { Invitation, PageResponse } from '@konfigyr/hooks/types';

const INVITATION: Invitation = {
  key: '0b9f514567f6cd9bb393a06388fc3dd7',
  organization: {
    ...namespaces.konfigyr,
  },
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
};

const list = http.get('http://localhost/api/account/invitations', ( ) => {
  const response: PageResponse<Invitation> = {
    data: [INVITATION],
    metadata: {
      number: 1,
      size: 1,
      total: 1,
      pages: 1,
    },
  };

  return HttpResponse.json(response);
});

const get = http.get('http://localhost/api/account/invitations/:key', ({ params }) => {
  if (INVITATION.key !== params.key) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Invitation with key '${params.key}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json(INVITATION);
});

const accept = http.post('http://localhost/api/account/invitations/:key', ({ params }) => {
  if (INVITATION.key !== params.key) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Invitation with key '${params.key}' not found.`,
    }, { status: 404 });
  }

  return new HttpResponse(null, { status: 204 });
});

export default [
  list,
  get,
  accept,
];
