export type KeysetState = 'ACTIVE' | 'INACTIVE' | 'PENDING_DESTRUCTION' | 'DESTROYED';
export type KeysetOperation = 'encrypt' | 'decrypt' | 'sign' | 'verify';

export interface Keyset {
  id: string;
  algorithm: string;
  state: KeysetState;
  name: string;
  description?: string;
  tags: Array<string>;
  createdAt?: string;
  updatedAt?: string;
  destroyedAt?: string;
}

export interface CreateKeyset {
  algorithm: string;
  name: string;
  description?: string;
  tags: Array<string>;
}

export interface KeysetSearchQuery extends Record<string, string | number | boolean | undefined> {
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
