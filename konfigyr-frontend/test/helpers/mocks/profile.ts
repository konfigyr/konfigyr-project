import type { Profile } from '@konfigyr/hooks/types';

export const development: Profile = {
  id: 'development',
  slug: 'development',
  name: 'Development',
  policy: 'UNPROTECTED',
  position: 1,
};

export const staging: Profile = {
  id: 'staging',
  slug: 'staging',
  name: 'Staging',
  policy: 'PROTECTED',
  position: 2,
};
