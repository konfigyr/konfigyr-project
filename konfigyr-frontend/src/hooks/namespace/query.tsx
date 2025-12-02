import { HTTPError } from 'ky';
import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useGetAccount } from '@konfigyr/hooks/account/query';
import request from '@konfigyr/lib/http';

import type { CreateNamespace, Namespace } from './types';

export const namespaceKeys = {
  getNamespaces: () => ['namespace'],
  getNamespace: (slug: string) => ['namespace', slug],
  getCheckNamespace: (slug: string) => ['namespace', 'check', slug],
};

export const getNamespacesQuery = () => {
  return queryOptions({
    queryKey: namespaceKeys.getNamespaces(),
    queryFn: async ({ signal }): Promise<Array<Namespace>> => {
      const response = await request.get('/api/namespaces/', { signal })
        .json<{ data: Array<Namespace> }>();

      return response.data;
    },
  });
};

export const useGetNamespaces = () => {
  return useQuery(getNamespacesQuery());
};

export const getNamespaceQuery = (slug: string) => {
  return queryOptions({
    queryKey: namespaceKeys.getNamespace(slug),
    queryFn: async ({ signal }): Promise<Namespace> => {
      return await request.get(`api/namespaces/${slug}`, { signal }).json<Namespace>();
    },
  });
};

export const useGetNamespace = (slug: string) => {
  return useQuery(getNamespaceQuery(slug));
};

export const checkNamespaceQuery = (slug: string) => {
  return queryOptions({
    queryKey: namespaceKeys.getCheckNamespace(slug),
    queryFn: async ({ signal }): Promise<{ exists: boolean }> => {
      try {
        const response = await request.head(`api/namespaces/${slug}`, { signal });
        return { exists: response.status === 200 };
      } catch (error) {
        if (error instanceof HTTPError && error.response.status === 404) {
          return { exists: false };
        }
        throw error;
      }
    },
  });
};

export const useCheckNamespace = (slug: string) => {
  return useQuery(checkNamespaceQuery(slug));
};

export const useCreateNamespace = () => {
  const client = useQueryClient();
  const { refetch: reloadAccount } = useGetAccount();

  return useMutation({
    mutationFn: async (payload: CreateNamespace): Promise<Namespace> => {
      const namespace = await request.post('api/namespaces', { json: payload })
        .json<Namespace>();

      // refetch the account to refresh the memberships once the namespace is created
      await reloadAccount();

      return namespace;
    },
    onSuccess(namespace: Namespace) {
      client.setQueryData(namespaceKeys.getNamespace(namespace.slug), namespace);
    },
  });
};
