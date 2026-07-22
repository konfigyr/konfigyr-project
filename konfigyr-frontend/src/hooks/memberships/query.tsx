import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';
import { useAccount } from '@konfigyr/hooks/account/context';
import { NamespaceRole } from './types';

import type { PageResponse, Pageable } from '@konfigyr/hooks/hateoas/types';
import type { Namespace } from '@konfigyr/hooks/namespace/types';
import type { Invitation, Member } from './types';

const DEFAULT_PAGEABLE: Pageable = { size: 100, page: 1 };

export const membershipKeys = {
  getAccountInvitations: (pageable: Pageable) => ['account', 'invitations', pageable],
  getAccountInvitation: (key: string) => ['account', 'invitation', key],
  getNamespaceInvitations: (slug: string, pageable: Pageable) => ['namespace', slug, 'invitations', pageable],
  getNamespaceInvitation: (slug: string, key: string) => ['namespace', slug, 'invitation', key],
  getNamespaceMembers: (slug: string) => ['namespace', slug, 'members'],
};

export const getAccountInvitations = () => {
  return queryOptions({
    queryKey: membershipKeys.getAccountInvitations(DEFAULT_PAGEABLE),
    queryFn: async ({ signal }): Promise<Array<Invitation>> => {
      const response = await request.get('api/account/invitations', { signal })
        .json<PageResponse<Invitation>>();
      return response.data;
    },
    placeholderData: previousData => previousData,
  });
};

export const useGetAccountInvitations = () => {
  return useQuery(getAccountInvitations());
};

export const getAccountInvitation = (key: string) => {
  return queryOptions({
    queryKey: membershipKeys.getAccountInvitation(key),
    queryFn: async ({ signal }): Promise<Invitation> => {
      return await request.get(`api/account/invitations/${key}`, { signal }).json();
    },
  });
};

export const useGetAccountInvitation = (key: string) => {
  return useQuery(getAccountInvitation(key));
};

export const useAcceptInvitation = () => {
  return useMutation({
    mutationFn: async (key: string) => {
      await request.post(`api/account/invitations/${key}`);
    },
  });
};

export const useDeclineInvitation = (key: string) => {
  return useMutation({
    mutationFn: async () => {
      await request.delete(`api/account/invitations/${key}`).json();
    },
  });
};

/* Namespace membership queries and mutations */

export const getNamespaceMembers = (namespace: Namespace) => {
  return queryOptions({
    queryKey: membershipKeys.getNamespaceMembers(namespace.slug),
    queryFn: async ({ signal }): Promise<Array<Member>> => {
      const response = await request.get(`api/namespaces/${namespace.slug}/members`, { signal })
        .json<PageResponse<Member>>();
      return response.data;
    },
    placeholderData: previousData => previousData,
  });
};

export const useGetNamespaceInvitations = (namespace: Namespace, pageable?: Pageable) => {
  return useQuery(getNamespaceInvitations(namespace, pageable));
};

export const getNamespaceInvitations = (namespace: Namespace, pageable: Pageable = {}) => {
  return queryOptions({
    queryKey: membershipKeys.getNamespaceInvitations(namespace.slug, pageable),
    queryFn: async ({ signal }): Promise<PageResponse<Invitation>> => {
      return await request.get(`api/namespaces/${namespace.slug}/invitations`, { signal, searchParams: { ...pageable } }).json();
    },
  });
};

export const getNamespaceInvitation = (namespace: Namespace, key: string) => {
  return queryOptions({
    queryKey: membershipKeys.getNamespaceInvitation(namespace.slug, key),
    queryFn: async ({ signal }): Promise<Invitation> => {
      return await request.get(`api/namespaces/${namespace.slug}/invitations/${key}`, { signal }).json();
    },
  });
};

export const useGetNamespaceMembers = (namespace: Namespace) => {
  return useQuery(getNamespaceMembers(namespace));
};

export const useCurrentNamespaceMember = (namespace: Namespace): Member | undefined => {
  const account = useAccount();
  const { data: members } = useGetNamespaceMembers(namespace);
  return members?.find(member => member.email === account.email);
};

export const useIsNamespaceAdmin = (namespace: Namespace): boolean => {
  const member = useCurrentNamespaceMember(namespace);
  return member?.role === NamespaceRole.ADMIN;
};

export const useInviteNamespaceMember = (namespace: Namespace) => {
  return useMutation({
    mutationFn: async ({ email, administrator }: { email: string, administrator: boolean }) => {
      return await request.post(`api/namespaces/${namespace.slug}/invitations`, {
        json: { email, role: administrator ? NamespaceRole.ADMIN : NamespaceRole.USER },
      }).json<Invitation>();
    },
  });
};

export const useUpdateNamespaceMember = (namespace: Namespace) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, role }: { id: string, role: NamespaceRole }) => {
      return await request.put(`api/namespaces/${namespace.slug}/members/${id}`, {
        json: { role },
      }).json<Member>();
    },
    onSuccess(member: Member) {
      client.setQueryData(membershipKeys.getNamespaceMembers(namespace.slug), (members?: Array<Member>) => {
        if (typeof members === 'undefined' || members.length === 0) {
          return [];
        }
        return members.map(it => it.id === member.id ? member : it);
      });
    },
  });
};

export const useRemoveNamespaceMember = (namespace: Namespace) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      return await request.delete(`api/namespaces/${namespace.slug}/members/${id}`)
        .then(() => id);
    },
    onSuccess(member: string) {
      client.setQueryData(membershipKeys.getNamespaceMembers(namespace.slug), (members?: Array<Member>) => {
        if (typeof members === 'undefined' || members.length === 0) {
          return [];
        }
        return members.filter(it => it.id !== member);
      });
    },
  });
};
