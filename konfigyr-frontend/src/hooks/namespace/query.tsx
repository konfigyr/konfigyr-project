import { HTTPError } from 'ky';
import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useGetAccount } from '@konfigyr/hooks/account/query';
import request from '@konfigyr/lib/http';
import { NamespaceRole } from './types';

import type {
  CreateNamespace,
  Invitation,
  Member,
  Namespace,
} from './types';

export const namespaceKeys = {
  getNamespaces: () => ['namespace'],
  getNamespace: (slug: string) => ['namespace', slug],
  getCheckNamespace: (slug: string) => ['namespace', 'check', slug],
  getNamespaceMembers: (slug: string) => ['namespace', slug, 'members'],
  getNamespaceInvitations: (slug: string) => ['namespace', slug, 'invitations'],
};

export const getNamespacesQuery = () => {
  return queryOptions({
    queryKey: namespaceKeys.getNamespaces(),
    queryFn: async ({ signal }): Promise<Array<Namespace>> => {
      const response = await request.get('api/namespaces', { signal })
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

export const useUpdateNamespace = (namespace: Namespace) => {
  const accountQuery = useGetAccount();
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (payload: CreateNamespace): Promise<Namespace> => {
      const updated = await request.put(`api/namespaces/${namespace.slug}`, {
        json: {
          name: payload.name ?? namespace.name,
          slug: payload.slug ?? namespace.slug,
          description: payload.description ?? namespace.description,
        },
      }).json<Namespace>();

      // refetch the account to refresh the memberships once the namespace is created
      await accountQuery.refetch();

      return updated;
    },
    onSuccess(updated: Namespace) {
      client.setQueryData(namespaceKeys.getNamespace(updated.slug), updated);
    },
  });
};

/* Namespace membership queries and mutations */

export const getNamespaceMembers = (slug: string) => {
  return queryOptions({
    queryKey: namespaceKeys.getNamespaceMembers(slug),
    queryFn: async (): Promise<Array<Member>> => {
      const response = await request.get(`api/namespaces/${slug}/members`)
        .json<{ data: Array<Member> }>();
      return response.data;
    },
  });
};

export const useGetNamespaceInvitations = (slug: string) => {
  return useQuery(getNamespaceInvitations(slug));
};

export const getNamespaceInvitations = (slug: string) => {
  return queryOptions({
    queryKey: namespaceKeys.getNamespaceInvitations(slug),
    queryFn: async (): Promise<Array<Invitation>> => {
      const response = await request.get(`api/namespaces/${slug}/invitations`)
        .json<{ data: Array<Invitation> }>();
      return response.data;
    },
  });
};

export const useGetNamespaceMembers = (slug: string) => {
  return useQuery(getNamespaceMembers(slug));
};

export const useInviteNamespaceMember = (slug: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ email, administrator } : { email: string, administrator: boolean }) => {
      return await request.post(`api/namespaces/${slug}/invitations`, {
        json: { email, role: administrator ? NamespaceRole.ADMIN : NamespaceRole.USER },
      }).json<Invitation>();
    },
  });
};

export const useUpdateNamespaceMember = (slug: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, role } : { id: string, role: NamespaceRole }) => {
      return await request.put(`api/namespaces/${slug}/members/${id}`, {
        json: { role },
      }).json<Member>();
    },
    onSuccess(member: Member) {
      client.setQueryData(namespaceKeys.getNamespaceMembers(slug), (members?: Array<Member>) => {
        if (typeof members === 'undefined' || members.length === 0) {
          return [];
        }
        return members.map(it => it.id === member.id ? member : it);
      });
    },
  });
};

export const useRemoveNamespaceMember = (slug: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      return await request.delete(`api/namespaces/${slug}/members/${id}`)
        .then(() => id);
    },
    onSuccess(member: string) {
      client.setQueryData(namespaceKeys.getNamespaceMembers(slug), (members?: Array<Member>) => {
        if (typeof members === 'undefined' || members.length === 0) {
          return [];
        }
        return members.filter(it => it.id !== member);
      });
    },
  });
};
