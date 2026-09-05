import { konfigyr } from './namespace';

import type { Service } from '@konfigyr/hooks/types';

export const konfigyrApi: Service = {
  id: '0789FPQ8AYDRH',
  namespace: konfigyr.id,
  slug: 'konfigyr-api',
  name:	'Konfigyr REST API',
  description: 'Konfigyr REST API service',
  createdAt: '2025-12-22',
  updatedAt: '2025-12-23',
};

export const konfigyrId: Service = {
  id: '0789GGCFPYDSK',
  namespace: konfigyr.id,
  slug: 'konfigyr-id',
  name:	'Konfigyr ID',
  description: 'Konfigyr identity server',
  createdAt: '2025-12-22',
  updatedAt: '2025-12-23',
};
