import type { Pageable } from '@konfigyr/hooks/hateoas/types';

export type TransferState = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

export interface TransferParty {
  id: string;
  slug: string;
}

export interface ArtifactOwnershipTransfer {
  id: string;
  groupId: string;
  from: TransferParty;
  to: TransferParty;
  state: TransferState;
  requestedAt: string | null;
  resolvedAt: string | null;
}

export interface ArtifactOwnershipTransferQuery extends Pageable {
  term?: string;
}

export interface RequestTransferPayload {
  groupId: string;
  fromNamespace: string;
}
