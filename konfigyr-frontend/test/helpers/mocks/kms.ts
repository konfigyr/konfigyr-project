import type { Keyset } from '@konfigyr/hooks/types';

export const encryptingKeyset: Keyset = {
  id: 'encrypting-keyset-id',
  state: 'ACTIVE',
  algorithm: 'AES256_GCM',
  name: 'encrypting-keyset',
  description: 'Keyset used for encrypting secrets',
  tags: [],
};

export const signingKeyset: Keyset = {
  id: 'signing-keyset-id',
  state: 'ACTIVE',
  algorithm: 'ECDSA_P256',
  name: 'signing-keyset',
  description: 'Keyset used for signing operations',
  tags: [],
};

export const disabledKeyset: Keyset = {
  id: 'disabled-keyset-id',
  state: 'INACTIVE',
  algorithm: 'AES256_GCM',
  name: 'disabled-keyset',
  description: 'Disabled keyset',
  tags: [],
};

export const destroyedKeyset: Keyset = {
  id: 'destroyed-keyset-id',
  state: 'PENDING_DESTRUCTION',
  algorithm: 'AES256_GCM',
  name: 'destroyed-keyset',
  description: 'Destroyed keyset',
  tags: [],
};
