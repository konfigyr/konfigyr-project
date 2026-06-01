import { addDays, subDays } from 'date-fns';

import type { Key, Keyset } from '@konfigyr/hooks/types';

interface MockKeyset extends Keyset {
  keys: Array<Key>;
}

export const encryptingKeyset: MockKeyset = {
  id: 'encrypting-keyset-id',
  state: 'ACTIVE',
  algorithm: 'AES256_GCM',
  name: 'encrypting-keyset',
  description: 'Keyset used for encrypting secrets',
  tags: [],
  keys: [
    {
      id: 'encrypting-keyset-id-1',
      status: 'ENABLED',
      isPrimary: true,
      algorithm: 'AES256_GCM',
    },
  ],
};

export const signingKeyset: MockKeyset = {
  id: 'signing-keyset-id',
  state: 'ACTIVE',
  algorithm: 'ECDSA_P256',
  name: 'signing-keyset',
  description: 'Keyset used for signing operations',
  tags: [],
  rotationInterval: 'PT720H',
  createdAt: subDays(new Date(), 210).toISOString(),
  updatedAt: subDays(new Date(), 30).toISOString(),
  keys: [
    {
      id: 'signing-keyset-id-2',
      status: 'ENABLED',
      isPrimary: true,
      algorithm: 'ECDSA_P256',
      createdAt: subDays(new Date(), 30).toISOString(),
      expiresAt: addDays(new Date(), 150).toISOString(),
    },
    {
      id: 'signing-keyset-id-1',
      status: 'ENABLED',
      isPrimary: false,
      algorithm: 'ECDSA_P521',
      createdAt: subDays(new Date(), 210).toISOString(),
      expiresAt: subDays(new Date(), 30).toISOString(),
    },
  ],
};

export const disabledKeyset: MockKeyset = {
  id: 'disabled-keyset-id',
  state: 'INACTIVE',
  algorithm: 'AES256_GCM',
  name: 'disabled-keyset',
  description: 'Disabled keyset',
  tags: [],
  createdAt: subDays(new Date(), 30).toISOString(),
  updatedAt: subDays(new Date(), 10).toISOString(),
  keys: [
    {
      id: 'disabled-keyset-id-1',
      status: 'DISABLED',
      isPrimary: true,
      algorithm: 'AES256_GCM',
      createdAt: subDays(new Date(), 30).toISOString(),
      expiresAt: addDays(new Date(), 60).toISOString(),
    },
  ],
};

export const destroyedKeyset: MockKeyset = {
  id: 'destroyed-keyset-id',
  state: 'PENDING_DESTRUCTION',
  algorithm: 'AES256_GCM',
  name: 'destroyed-keyset',
  description: 'Destroyed keyset',
  tags: [],
  rotationInterval: 'PT720H',
  createdAt: subDays(new Date(), 90).toISOString(),
  updatedAt: subDays(new Date(), 60).toISOString(),
  keys: [
    {
      id: 'destroyed-keyset-id-1',
      status: 'PENDING_DESTRUCTION',
      isPrimary: true,
      algorithm: 'AES256_GCM',
      createdAt: subDays(new Date(), 90).toISOString(),
      expiresAt: addDays(new Date(), 90).toISOString(),
      destructionScheduledAt: subDays(new Date(), 1).toISOString(),
      destroyedAt: subDays(new Date(), 1).toISOString(),
    },
  ],
};
