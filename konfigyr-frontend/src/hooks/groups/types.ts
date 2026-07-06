import type { Pageable } from '@konfigyr/hooks/hateoas/types';

export type VerificationState = 'PENDING' | 'ACTIVE' | 'REVOKED' | 'FAILED';
export type VerificationMethod = 'DNS' | 'SOURCE_CODE';
export type ChallengeState = 'UNVERIFIED' | 'VERIFIED' | 'EXPIRED';

export interface GroupVerification {
  id: string;
  groupId: string;
  state: VerificationState;
  createdAt: string;
  verifiedAt: string | null;
  revokedAt: string | null;
}

export interface VerificationChallenge {
  id: string;
  verificationId: string;
  method: VerificationMethod;
  token: string;
  state: ChallengeState;
  createdAt: string | null;
  verifiedAt: string | null;
  expiresAt: string | null;
}

export interface GroupVerificationQuery extends Pageable {
  term?: string;
  state?: VerificationState;
}
