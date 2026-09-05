import type { GroupVerification, VerificationChallenge } from '@konfigyr/hooks/groups/types';

export const activeVerification: GroupVerification = {
  id: 'verification-active',
  groupId: 'com.example.group',
  state: 'ACTIVE',
  createdAt: '2026-07-07T00:00:00Z',
  verifiedAt: '2026-07-08T00:00:00Z',
  revokedAt: null,
};

export const pendingVerification: GroupVerification = {
  id: 'verification-pending',
  groupId: 'io.github.acme',
  state: 'PENDING',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  revokedAt: null,
};

export const conflictedVerification: GroupVerification = {
  id: 'verification-conflicted',
  groupId: 'com.acme.widgets',
  state: 'PENDING',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  revokedAt: null,
  conflictingOwners: ['ebf'],
};

export const activeConflictedVerification: GroupVerification = {
  id: 'verification-active-conflicted',
  groupId: 'com.acme.transfer-ready',
  state: 'ACTIVE',
  createdAt: '2026-07-05T00:00:00Z',
  verifiedAt: '2026-07-06T00:00:00Z',
  revokedAt: null,
  conflictingOwners: ['ebf'],
};

export const verificationChallenge: VerificationChallenge = {
  id: 'challenge-pending',
  verificationId: pendingVerification.id,
  method: 'SOURCE_CODE',
  token: 'token-123',
  state: 'UNVERIFIED',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  expiresAt: null,
};

export const conflictedChallenge: VerificationChallenge = {
  id: 'challenge-conflicted',
  verificationId: conflictedVerification.id,
  method: 'DNS',
  token: 'token-456',
  state: 'UNVERIFIED',
  createdAt: '2026-07-09T00:00:00Z',
  verifiedAt: null,
  expiresAt: null,
};
