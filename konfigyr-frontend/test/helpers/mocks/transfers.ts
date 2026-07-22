import { konfigyr } from './namespace';

import type { ArtifactOwnershipTransfer } from '@konfigyr/hooks/transfers/types';

export const incomingPending: ArtifactOwnershipTransfer = {
  id: 'transfer-incoming-pending',
  groupId: 'com.example.group',
  from: { id: '2', slug: konfigyr.slug },
  to: { id: '99', slug: 'ebf' },
  state: 'PENDING',
  requestedAt: '2026-07-10T00:00:00Z',
  resolvedAt: null,
};

export const outgoingPending: ArtifactOwnershipTransfer = {
  id: 'transfer-outgoing-pending',
  groupId: 'io.github.acme',
  from: { id: '99', slug: 'ebf' },
  to: { id: '2', slug: konfigyr.slug },
  state: 'PENDING',
  requestedAt: '2026-07-11T00:00:00Z',
  resolvedAt: null,
};

export const resolvedTransfer: ArtifactOwnershipTransfer = {
  id: 'transfer-resolved',
  groupId: 'com.acme.widgets',
  from: { id: '2', slug: konfigyr.slug },
  to: { id: '99', slug: 'ebf' },
  state: 'ACCEPTED',
  requestedAt: '2026-07-05T00:00:00Z',
  resolvedAt: '2026-07-06T00:00:00Z',
};

export const forbiddenTransfer: ArtifactOwnershipTransfer = {
  id: 'transfer-forbidden',
  groupId: 'com.forbidden.group',
  from: { id: '99', slug: 'ebf' },
  to: { id: '100', slug: 'other-namespace' },
  state: 'PENDING',
  requestedAt: '2026-07-12T00:00:00Z',
  resolvedAt: null,
};
