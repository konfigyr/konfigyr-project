import { parseISO } from 'date-fns';
import { NamespaceRole } from '@konfigyr/hooks/memberships/types';
import * as accounts from './account';
import * as namespaces from './namespace';

import type { Invitation } from '@konfigyr/hooks/types';

export const konfigyr: Invitation = {
  key: '0b9f514567f6cd9bb393a06388fc3dd7',
  organization: {
    ...namespaces.konfigyr,
  },
  sender: {
    id: accounts.johnDoe.id,
    email: accounts.johnDoe.email,
    name: accounts.johnDoe.fullName,
  },
  recipient: {
    email: 'recipient-user@konfigyr.com',
  },
  role: NamespaceRole.USER,
  expired: false,
  createdAt: parseISO('2026-04-17T09:45:12'),
  expiryDate: parseISO('2026-04-28T19:45:16'),
};

export const johnDoe: Invitation = {
  key: '9456532868b6cdd235703d7d09eb302',
  organization: {
    ...namespaces.johnDoe,
  },
  sender: {
    id: accounts.janeDoe.id,
    email: accounts.janeDoe.email,
    name: accounts.janeDoe.fullName,
  },
  recipient: {
    email: 'recipient-user@konfigyr.com',
  },
  role: NamespaceRole.ADMIN,
  expired: false,
  createdAt: parseISO('2026-04-11T11:30:30'),
  expiryDate: parseISO('2026-04-17T16:30:30'),
};
