import { subMinutes } from 'date-fns';
import { ChangeRequestMergeStatus, ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { konfigyrApi } from './services';
import { development, staging } from './profile';

import type { ChangeRequest } from '@konfigyr/hooks/vault/types';

export const firstChangeRequest: ChangeRequest = {
  id: 'first-change-request',
  service: konfigyrApi,
  profile: development,
  number: 1,
  state: ChangeRequestState.OPEN,
  mergeStatus: ChangeRequestMergeStatus.MERGEABLE,
  subject: 'Update application name',
  description: {
    html: '<p>Align the application name with the new naming convention</p>',
    markdown: 'Align the application name with the new naming convention',
  },
  count: 3,
  createdBy: 'John Doe',
  createdAt: subMinutes(new Date(), 12).toISOString(),
  updatedAt: subMinutes(new Date(), 7).toISOString(),
};

export const secondChangeRequest: ChangeRequest = {
  id: 'second-change-request',
  service: konfigyrApi,
  profile: staging,
  number: 2,
  state: ChangeRequestState.OPEN,
  mergeStatus: ChangeRequestMergeStatus.MERGEABLE,
  subject: 'Update datasource URL',
  description: {
    html: '<p>Point to the staging database</p>',
    markdown: 'Point to the staging database',
  },
  count: 1,
  createdBy: 'John Doe',
  createdAt: subMinutes(new Date(), 7).toISOString(),
  updatedAt: subMinutes(new Date(), 2).toISOString(),
};
