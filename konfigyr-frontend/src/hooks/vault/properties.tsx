import { useMemo } from 'react';
import { useDebounce } from 'use-debounce';
import { queryOptions, useQuery } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';
import type { CursorResponse } from '@konfigyr/hooks/hateoas/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeHistoryRecord, Profile } from '@konfigyr/hooks/vault/types';

/**
 * Keys used to store the configuration properties in the query client.
 */
export const propertyKeys = {
  getPropertyHistory: (profile: Profile, name: string) => ['vault', profile.id, 'property', name, 'history'],
};

export const getPropertyHistoryQuery = (namespace: Namespace, service: Service, profile: Profile, name: string) => {
  return queryOptions({
    queryKey: propertyKeys.getPropertyHistory(profile, name),
    queryFn: async (): Promise<CursorResponse<ChangeHistoryRecord>> => {
      return await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/property/${name}/history`)
        .json<CursorResponse<ChangeHistoryRecord>>();
    },
  });
};

export const useGetPropertyHistory = (namespace: Namespace, service: Service, profile: Profile, name: string) => {
  return useQuery(getPropertyHistoryQuery(namespace, service, profile, name));
};

/* Search property metadata hooks */

/**
 * Splits the given search term into an array of tokens that would be used to search for properties.
 *
 * @param term the search term to split
 * @returns {Array<string>} an array of tokens
 */
export function splitSearchTerm(term?: string): Array<string> {
  if (typeof term !== 'string') {
    return [];
  }
  return term.trim().toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .map(token => token.trim());
}

/**
 * Hook that would convert the input search term into an array of tokens that would be used to search the
 * property descriptors. This hook would also debounce the input search term using the specified delay
 * and would return the resulting array of tokens.
 *
 * @param term the search term to split
 * @param delay the delay in milliseconds to debounce the input search term
 * @returns {Array<string>} an array of tokens
 */
export function useSplitSearchTerm(term?: string, delay: number = 200): Array<string> {
  const [debounced] = useDebounce(term, delay);
  return useMemo(() => splitSearchTerm(debounced), [debounced]);
}

/**
 * Filters the given property descriptors based on the given search terms. The search term should be split
 * beforehand using the `splitSearchTerm` function. The resulting array of tokens is then used to filter the
 * descriptor using the following rules:
 *  - property name matches the exact term would produce the highest score of 100
 *  - property name contains the term would produce the score of 10
 *  - property description contains the term would produce the score of 1
 *
 * The total score of the property descriptor match is then used to sort the descriptors in descending order,
 * showing the most relevant properties first.
 *
 * @param properties the property descriptors to filter
 * @param terms the search terms to filter the descriptors
 * @returns {Array} the filtered property descriptors with their scores
 */
export function filterPropertyDescriptors<T extends PropertyDescriptor>(
  properties: Array<T> = [],
  terms: Array<string> = [],
): Array<T & { score?: number }> {
  if (properties.length === 0 || terms.length === 0) {
    return properties;
  }

  return properties
    .map(property => {
      let score = 0;
      const name = property.name.toLowerCase();
      const description = property.description?.toLowerCase() || '';

      terms.forEach(term => {
        if (name === term) {
          score += 100; // exact match, the highest score
        } else if (name.includes(term)) {
          score += 10; // partial match, slightly higher score
        }

        if (description.includes(term)) {
          score += 1; // description partial matches, lowest score
        }
      });

      return { ...property, score };
    })
    .filter(it => it.score > 0)
    .sort((a, b) => b.score - a.score);
}

export function usePropertyDescriptorSearch<T extends PropertyDescriptor>(
  properties: Array<T> = [],
  term?: string,
  delay: number = 200,
): Array<T & { score?: number }> {
  const tokens = useSplitSearchTerm(term, delay);
  return useMemo(() => filterPropertyDescriptors(properties, tokens), [properties, tokens]);
}
