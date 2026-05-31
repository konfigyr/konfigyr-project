import type { Pageable } from '../hateoas/types';

export type KeyType = 'EC' | 'RSA' | 'OCTET';
export type KeyStatus =
  'INITIALIZING' |
  'INITIALIZATION_FAILED' |
  'ENABLED' |
  'COMPROMISED' |
  'DISABLED' |
  'PENDING_DESTRUCTION' |
  'DESTROYED' |
  'DESTRUCTION_FAILED';

export type KeysetPurpose = 'ENCRYPTION' | 'SIGNING';
export type KeysetState = 'ACTIVE' | 'INACTIVE' | 'PENDING_DESTRUCTION' | 'DESTROYED';
export type KeysetOperation = 'encrypt' | 'decrypt' | 'sign' | 'verify';

export interface KeysetAlgorithm {
  name: string;
  type: KeyType;
  purpose: KeysetPurpose;
  operations: Array<KeysetOperation>;
}

export interface Keyset {
  id: string;
  algorithm: string;
  state: KeysetState;
  name: string;
  description?: string;
  tags: Array<string>;
  rotationInterval?: string;
  destructionGracePeriod?: string;
  createdAt?: string;
  updatedAt?: string;
  destroyedAt?: string;
}

export interface Key {
  id: string;
  algorithm: string;
  status: KeyStatus;
  isPrimary: boolean;
  createdAt?: string;
  initializedAt?: string;
  expiresAt?: string;
  destructionScheduledAt?: string;
  destroyedAt?: string;
}

export interface CreateKeyset {
  algorithm: string;
  name: string;
  description?: string;
  tags: Array<string>;
}

export interface KeysetSearchQuery extends Pageable {
  term?: string;
  state?: KeysetState;
  algorithm?: string;
  sort?: string;
  page?: number;
}

export interface PlaintextAware {
  plaintext: string;
}

export interface CiphertextAware {
  ciphertext: string;
}

export interface SignatureAware {
  signature: string;
}

export interface AdditionalAuthenticationDataAware {
  aad?: string;
}

export interface KeysetEncryptOperationResponse extends CiphertextAware {
  checksum: string;
}

export interface KeysetDecryptOperationResponse extends PlaintextAware {
  plaintext: string;
}

export interface KeysetVerificationOperationResponse extends PlaintextAware {
  valid: boolean;
}

export interface KeysetOperationRequests {
  'encrypt': PlaintextAware & AdditionalAuthenticationDataAware,
  'decrypt': CiphertextAware & AdditionalAuthenticationDataAware,
  'sign': PlaintextAware,
  'verify': PlaintextAware & SignatureAware,
}

export interface KeysetOperationResponses {
  'encrypt': KeysetEncryptOperationResponse,
  'decrypt': KeysetDecryptOperationResponse,
  'sign': SignatureAware,
  'verify': KeysetVerificationOperationResponse,
}
