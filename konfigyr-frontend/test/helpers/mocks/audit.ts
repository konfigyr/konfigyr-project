import { janeDoe, johnDoe } from './account';
import { konfigyr } from './namespace';
import { incomingPending } from './transfers';

import type { AuditRecord } from '@konfigyr/hooks/types';

export const keysetCreated: AuditRecord = {
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
};

export const profileCreated: AuditRecord = {
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
};

export const serviceCreated: AuditRecord = {
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
};

export const namespaceCreated: AuditRecord = {
  id: 'first-audit-record',
  entityType: 'namespace',
  entityId: konfigyr.id,
  eventType: 'namespace.created',
  message: 'Namespace was created',
  actor: {
    id: johnDoe.id,
    type: 'USER_ACCOUNT',
    name: johnDoe.fullName ?? johnDoe.email,
  },
  createdAt: '2026-04-28T20:51:39.399083+02:00',
};

export const transferRequested: AuditRecord = {
  id: 'transfer-requested-audit-record',
  entityType: 'artifact-ownership-transfer',
  entityId: incomingPending.id,
  eventType: 'artifact-ownership-transfer.requested',
  message: 'Ownership transfer for com.example.group was requested from ebf',
  actor: {
    id: johnDoe.id,
    type: 'USER_ACCOUNT',
    name: johnDoe.fullName ?? johnDoe.email,
  },
  createdAt: '2026-07-10T00:00:00Z',
};

export const allRecords: Array<AuditRecord> = [
  keysetCreated,
  profileCreated,
  serviceCreated,
  namespaceCreated,
  transferRequested,
];
