import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { CreateProfile, Profile } from '@konfigyr/hooks/vault/types';

/**
 * Keys used to store the configuration properties in the query client.
 */
export const serviceProfileKeys = {
  getProfiles: (namespace: Namespace, service: Service) => [
    'namespace', namespace.slug, 'services', service.slug, 'profiles',
  ],
  getProfile: (namespace: Namespace, service: Service, slug: string) => [
    'namespace', namespace.slug, 'services', service.slug, 'profiles', slug,
  ],
};

export const getProfilesQuery = (namespace: Namespace, service: Service) => {
  return queryOptions({
    queryKey: serviceProfileKeys.getProfiles(namespace, service),
    queryFn: async ({ signal }): Promise<Array<Profile>> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/profiles`;
      const response = await request.get(uri, { signal })
        .json<{ data: Array<Profile> }>();

      return response.data;
    },
  });
};

export const useGetProfiles = (namespace: Namespace, service: Service) => {
  return useQuery(getProfilesQuery(namespace, service));
};

export const getProfileQuery = (namespace: Namespace, service: Service, slug: string) => {
  return queryOptions({
    queryKey: serviceProfileKeys.getProfile(namespace, service, slug),
    queryFn: async ({ signal }) => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${slug}`;
      return await request.get(uri, { signal }).json<Profile>();
    },
  });
};

export const useGetProfile = (namespace: Namespace, service: Service, slug: string) => {
  return useQuery(getProfileQuery(namespace, service, slug));
};

export const useCreateProfile = (namespace: Namespace, service: Service) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateProfile): Promise<Profile> => {
      return request.post(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles`, { json: payload })
        .json<Profile>();
    },
    onSuccess(profile: Profile) {
      client.setQueryData(serviceProfileKeys.getProfile(namespace, service, profile.slug), profile);
    },
    async onSettled() {
      await client.fetchQuery(getProfilesQuery(namespace, service));
    },
  });
};
