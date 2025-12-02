import type { Account } from '@konfigyr/hooks/types';

export const johnDoe: Account = {
  id: '06Y7W2BYKG9B9',
  email: 'john.doe@konfigyr.com',
  firstName: 'John',
  lastName: 'Doe',
  fullName: 'John Doe',
  avatar: 'https://avatar.vercel.sh/06Y7W2BYKG9B9.svg?text=VS',
  memberships: [
    {
      id: '1',
      namespace: 'john-doe',
      name: 'John Doe',
      role: 'ADMIN',
      since: '2025-12-01',
    },
    {
      id: '2',
      namespace: 'konfigyr',
      name: 'Konfigyr',
      role: 'ADMIN',
      since: '2025-12-02',
    },
  ],
};

export const janeDoe: Account = {
  id: '2',
  email: 'jane.doe@konfigyr.com',
  firstName: 'Jane',
  lastName: 'Doe',
  fullName: 'Jane Doe',
  memberships: [
    {
      id: '2',
      namespace: 'konfigyr',
      name: 'Konfigyr',
      role: 'USER',
      since: '2025-12-02',
    },
  ],
};
