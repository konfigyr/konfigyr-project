import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
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
        .json<PageResponse<Profile>>();

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

export const useUpdateProfile = (namespace: Namespace, service: Service, profile: Profile) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, name, description, policy }: { id: string, name?: string, description?: string, policy?: string }) => {
      return await request.put(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}`, {
        json: {
          id,
          name,
          description,
          policy,
        },
      }).json<Profile>();
    },
    onSuccess(updated: Profile) {
      client.setQueryData(serviceProfileKeys.getProfiles(namespace, service), (profiles?: Array<Profile>) => {
        if (typeof profiles === 'undefined' || profiles.length === 0) {
          return [];
        }
        return profiles.map(it => it.id === updated.id ? updated : it);
      });
      client.setQueryData(serviceProfileKeys.getProfile(namespace, service, updated.slug), updated);
    },
  });
};

export const useRemoveProfile = (namespace: Namespace, service: Service) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (slug: string) => {
      return await request.delete(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${slug}`)
        .then(() => slug);
    },
    onSuccess(slug: string) {
      client.setQueryData(serviceProfileKeys.getProfiles(namespace, service), (profiles?: Array<Profile>) => {
        if (typeof profiles === 'undefined' || profiles.length === 0) {
          return [];
        }
        return profiles.filter(it => it.slug !== slug);
      });
      client.removeQueries({
        queryKey: serviceProfileKeys.getProfile(namespace, service, slug),
      });
    },
  });
};
