import type { Pageable } from '@konfigyr/hooks/hateoas/types';

export type VerificationState = 'PENDING' | 'ACTIVE' | 'REVOKED' | 'FAILED';
export type VerificationMethod = 'DNS' | 'SOURCE_CODE';

export interface GroupVerification {
  id: string;
  groupId: string;
  state: VerificationState;
  createdAt: string;
  verifiedAt: string | null;
  revokedAt: string | null;
}

export interface GroupVerificationQuery extends Pageable {
  term?: string;
  state?: VerificationState;
}
