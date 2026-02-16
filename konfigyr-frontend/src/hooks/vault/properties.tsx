import { queryOptions, useQuery } from '@tanstack/react-query';

import metadata from './metadata.json';

import type { ChangeHistoryRecord, Profile, PropertyDescriptor } from '@konfigyr/hooks/vault/types';

/* Default history of the property changes. Should be replaced with a real API call. */
const history: Array<ChangeHistoryRecord> = [{
  id: '1',
  timestamp: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
  user: 'alex.novak@configvault.io',
  action: 'modified',
  previousValue: '10',
  newValue: '20',
}, {
  id: '2',
  timestamp: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
  user: 'ci-pipeline@github',
  action: 'modified',
  previousValue: '5',
  newValue: '10',
}, {
  id: '3',
  timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(),
  user: 'alex.novak@configvault.io',
  action: 'created',
  newValue: '5',
}];

/**
 * Keys used to store the configuration properties in the query client.
 */
export const propertyKeys = {
  searchPropertyMetadata: (profile: Profile, term: string) => ['vault', profile.id, 'property-metadata', { term }],
  getPropertyHistory: (profile: Profile, name: string) => ['vault', profile.id, 'property', name, 'history'],
};

export const getHistoryQuery = (profile: Profile, name: string) => {
  return queryOptions({
    queryKey: propertyKeys.getPropertyHistory(profile, name),
    queryFn: async () => {
      await new Promise(resolve => setTimeout(resolve, 600));
      return history;
    },
  });
};

export const useGetHistory = (profile: Profile, name: string) => {
  return useQuery(getHistoryQuery(profile, name));
};

export const searchPropertyMetadataQuery = (profile: Profile, term: string) => {
  return queryOptions({
    queryKey: propertyKeys.searchPropertyMetadata(profile, term),
    placeholderData: state => state,
    queryFn: async () => {
      if (term.length < 3) {
        return [];
      }

      await new Promise(resolve => setTimeout(resolve, 200));
      const matches: Array<PropertyDescriptor> = [];

      for (const property of metadata.properties) {
        if (property.name.toLowerCase().includes(term.toLowerCase())) {
          matches.push(property as any as PropertyDescriptor);
        }

        if (matches.length === 10) {
          break;
        }
      }

      return matches;
    },
  });
};

export const useSearchPropertyMetadata = (profile: Profile, term: string) => {
  return useQuery(searchPropertyMetadataQuery(profile, term));
};
