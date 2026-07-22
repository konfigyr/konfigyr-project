import { HttpResponse, http } from 'msw';
import { namespaces } from '../../mocks';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { ArtifactOwnershipTransfer } from '@konfigyr/hooks/transfers/types';

const incomingPending: ArtifactOwnershipTransfer = {
  id: 'transfer-incoming-pending',
  groupId: 'com.example.group',
  from: { id: '2', slug: namespaces.konfigyr.slug },
  to: { id: '99', slug: 'ebf' },
  state: 'PENDING',
  requestedAt: '2026-07-10T00:00:00Z',
  resolvedAt: null,
};

const outgoingPending: ArtifactOwnershipTransfer = {
  id: 'transfer-outgoing-pending',
  groupId: 'io.github.acme',
  from: { id: '99', slug: 'ebf' },
  to: { id: '2', slug: namespaces.konfigyr.slug },
  state: 'PENDING',
  requestedAt: '2026-07-11T00:00:00Z',
  resolvedAt: null,
};

const resolvedTransfer: ArtifactOwnershipTransfer = {
  id: 'transfer-resolved',
  groupId: 'com.acme.widgets',
  from: { id: '2', slug: namespaces.konfigyr.slug },
  to: { id: '99', slug: 'ebf' },
  state: 'ACCEPTED',
  requestedAt: '2026-07-05T00:00:00Z',
  resolvedAt: '2026-07-06T00:00:00Z',
};

const forbiddenTransfer: ArtifactOwnershipTransfer = {
  id: 'transfer-forbidden',
  groupId: 'com.forbidden.group',
  from: { id: '99', slug: 'ebf' },
  to: { id: '100', slug: 'other-namespace' },
  state: 'PENDING',
  requestedAt: '2026-07-12T00:00:00Z',
  resolvedAt: null,
};

const transfersById = new Map<string, ArtifactOwnershipTransfer>([
  [incomingPending.id, incomingPending],
  [outgoingPending.id, outgoingPending],
  [resolvedTransfer.id, resolvedTransfer],
  [forbiddenTransfer.id, forbiddenTransfer],
]);

const incoming = http.get('http://localhost/api/namespaces/:slug/artifact-transfers/incoming', ({ params }) => {
  const { slug } = params;
  const data = slug === namespaces.konfigyr.slug ? [incomingPending] : [];

  const response: PageResponse<ArtifactOwnershipTransfer> = {
    data,
    metadata: { number: 1, size: 20, total: data.length, pages: 1 },
  };

  return HttpResponse.json(response);
});

const outgoing = http.get('http://localhost/api/namespaces/:slug/artifact-transfers/outgoing', ({ params }) => {
  const { slug } = params;
  const data = slug === namespaces.konfigyr.slug ? [outgoingPending] : [];

  const response: PageResponse<ArtifactOwnershipTransfer> = {
    data,
    metadata: { number: 1, size: 20, total: data.length, pages: 1 },
  };

  return HttpResponse.json(response);
});

const get = http.get('http://localhost/api/namespaces/:slug/artifact-transfers/:id', ({ params }) => {
  const { id } = params;
  const transfer = transfersById.get(id as string);

  if (!transfer) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Could not find an artifact ownership transfer '${id}'.`,
    }, { status: 404 });
  }

  return HttpResponse.json(transfer);
});

const create = http.post('http://localhost/api/namespaces/:slug/artifact-transfers', async ({ request }) => {
  const body = await request.clone().json() as { groupId?: string; fromNamespace?: string };

  if (body.groupId === 'unverified.group') {
    return HttpResponse.json({
      status: 400,
      title: 'Bad request',
      detail: `GroupId '${body.groupId}' is not verified for publishing`,
    }, { status: 400 });
  }

  const response: ArtifactOwnershipTransfer = {
    id: `${body.groupId}-transfer`,
    groupId: body.groupId!,
    from: { id: '99', slug: body.fromNamespace! },
    to: { id: '2', slug: namespaces.konfigyr.slug },
    state: 'PENDING',
    requestedAt: '2026-07-13T00:00:00Z',
    resolvedAt: null,
  };

  return HttpResponse.json(response, { status: 201 });
});

const accept = http.post('http://localhost/api/namespaces/:slug/artifact-transfers/:id/accept', ({ params }) => {
  const { id } = params;
  const transfer = transfersById.get(id as string);

  if (!transfer) {
    return HttpResponse.json({ status: 404, title: 'Not found', detail: `Could not find transfer '${id}'.` }, { status: 404 });
  }

  if (id === forbiddenTransfer.id) {
    return HttpResponse.json({
      status: 403,
      title: 'Access Denied',
      detail: `Only the '${transfer.from.slug}' namespace may accept this transfer`,
    }, { status: 403 });
  }

  if (transfer.state !== 'PENDING') {
    return HttpResponse.json({
      status: 409,
      title: 'Conflict',
      detail: `Artifact ownership transfer '${id}' has already been resolved with state '${transfer.state}'`,
    }, { status: 409 });
  }

  const updated: ArtifactOwnershipTransfer = { ...transfer, state: 'ACCEPTED', resolvedAt: '2026-07-14T00:00:00Z' };
  transfersById.set(updated.id, updated);

  return HttpResponse.json(updated);
});

const reject = http.post('http://localhost/api/namespaces/:slug/artifact-transfers/:id/reject', ({ params }) => {
  const { id } = params;
  const transfer = transfersById.get(id as string);

  if (!transfer) {
    return HttpResponse.json({ status: 404, title: 'Not found', detail: `Could not find transfer '${id}'.` }, { status: 404 });
  }

  const updated: ArtifactOwnershipTransfer = { ...transfer, state: 'REJECTED', resolvedAt: '2026-07-14T00:00:00Z' };
  transfersById.set(updated.id, updated);

  return HttpResponse.json(updated);
});

const cancel = http.delete('http://localhost/api/namespaces/:slug/artifact-transfers/:id', ({ params }) => {
  const { id } = params;
  const transfer = transfersById.get(id as string);

  if (!transfer) {
    return HttpResponse.json({ status: 404, title: 'Not found', detail: `Could not find transfer '${id}'.` }, { status: 404 });
  }

  const updated: ArtifactOwnershipTransfer = { ...transfer, state: 'CANCELLED', resolvedAt: '2026-07-14T00:00:00Z' };
  transfersById.set(updated.id, updated);

  return HttpResponse.json(updated);
});

export default [
  incoming,
  outgoing,
  get,
  create,
  accept,
  reject,
  cancel,
];
