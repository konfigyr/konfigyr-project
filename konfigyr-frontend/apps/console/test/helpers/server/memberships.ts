import { HttpResponse, http } from 'msw';
import { invitations } from '../mocks';

import type { Invitation, PageResponse } from '@konfigyr/hooks/types';

const ACCOUNT_INVITATIONS: Record<string, Invitation | undefined> = Object.values(invitations)
  .reduce((state, invitation) => ({
    ...state, [invitation.key]: invitation,
  }), {});

const list = http.get('http://localhost/api/account/invitations', () => {
  const data = Object.values(ACCOUNT_INVITATIONS) as Array<Invitation>;

  const response: PageResponse<Invitation> = {
    data,
    metadata: {
      number: 1,
      size: data.length,
      total: data.length,
      pages: 1,
    },
  };

  return HttpResponse.json(response);
});

const get = http.get('http://localhost/api/account/invitations/:key', ({ params }) => {
  const invitation = ACCOUNT_INVITATIONS[params.key as string];

  if (!invitation) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Invitation with key '${params.key}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json(invitation);
});

const accept = http.post('http://localhost/api/account/invitations/:key', ({ params }) => {
  if (!ACCOUNT_INVITATIONS[params.key as string]) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Invitation with key '${params.key}' not found.`,
    }, { status: 404 });
  }

  return new HttpResponse(null, { status: 204 });
});

const decline = http.delete('http://localhost/api/account/invitations/:key', ({ params }) => {
  if (!ACCOUNT_INVITATIONS[params.key as string]) {
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
  decline,
];
