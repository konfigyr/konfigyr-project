import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { CollectionResponse, PageResponse } from '@konfigyr/hooks/hateoas/types';
import type {
  GroupVerification,
  GroupVerificationQuery,
  VerificationChallenge,
  VerificationMethod,
} from './types';

export const groupVerificationKeys = {
  getGroupVerification: (namespace: string, groupId: string) => ['namespace', namespace, 'group-verifications', groupId],
  getVerificationChallenges: (namespace: string, groupId: string) => ['namespace', namespace, 'group-verifications', groupId, 'challenges'],
  getGroupVerifications: (namespace: string, query: GroupVerificationQuery) => ['namespace', namespace, 'group-verifications', query],
};

export const getGroupVerifications = (namespace: string, query: GroupVerificationQuery = {}) => {
  return queryOptions({
    queryKey: groupVerificationKeys.getGroupVerifications(namespace, query),
    queryFn: async ({ signal }): Promise<PageResponse<GroupVerification>> => {
      return await request
        .get(`api/namespaces/${namespace}/group-verifications`, { signal, searchParams: { ...query } })
        .json();
    },
    placeholderData: previousData => previousData,
  });
};

export const getGroupVerification = (namespace: string, groupId: string) => {
  return queryOptions({
    queryKey: groupVerificationKeys.getGroupVerification(namespace, groupId),
    queryFn: async ({ signal }): Promise<GroupVerification> => {
      return await request
        .get(`api/namespaces/${namespace}/group-verifications/${groupId}`, { signal })
        .json();
    },
  });
};

export const getVerificationChallenges = (namespace: string, groupId: string) => {
  return queryOptions({
    queryKey: groupVerificationKeys.getVerificationChallenges(namespace, groupId),
    queryFn: async ({ signal }): Promise<CollectionResponse<VerificationChallenge>> => {
      return await request
        .get(`api/namespaces/${namespace}/group-verifications/${groupId}/challenges`, { signal })
        .json();
    },
  });
};

export const useGetGroupVerifications = (namespace: string, query?: GroupVerificationQuery) => {
  return useQuery(getGroupVerifications(namespace, query));
};

export const useGetGroupVerification = (namespace: string, groupId: string) => {
  return useQuery(getGroupVerification(namespace, groupId));
};

export const useGetVerificationChallenges = (namespace: string, groupId: string) => {
  return useQuery(getVerificationChallenges(namespace, groupId));
};

export const useClaimGroupVerification = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (payload: { groupId: string; verificationMethod: VerificationMethod }): Promise<GroupVerification> => {
      return request.post(`api/namespaces/${namespace}/group-verifications`, {
        json: payload,
      }).json<GroupVerification>();
    },
    onSuccess: (verification) => {
      client.setQueryData(groupVerificationKeys.getGroupVerification(namespace, verification.groupId), verification);
    },
  });
};

export const useClaimAgainGroupVerification = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (payload: { groupId: string; verificationMethod: VerificationMethod }): Promise<GroupVerification> => {
      return request.put(`api/namespaces/${namespace}/group-verifications`, {
        json: payload,
      }).json<GroupVerification>();
    },
    onSuccess: async (verification) => {
      client.setQueryData(groupVerificationKeys.getGroupVerification(namespace, verification.groupId), verification);
      await client.invalidateQueries({
        queryKey: groupVerificationKeys.getVerificationChallenges(namespace, verification.groupId),
      });
    },
  });
};

export const useVerifyGroupVerification = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (groupId: string): Promise<GroupVerification> => {
      return request.post(`api/namespaces/${namespace}/group-verifications/${groupId}/verify`).json<GroupVerification>();
    },
    onSuccess: async (verification) => {
      client.setQueryData(groupVerificationKeys.getGroupVerification(namespace, verification.groupId), verification);
      await client.invalidateQueries({
        queryKey: groupVerificationKeys.getVerificationChallenges(namespace, verification.groupId),
      });
    },
  });
};

export const useRevokeGroupVerification = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (groupId: string) => {
      await request.delete(`api/namespaces/${namespace}/group-verifications/${groupId}`);
    },
    onSuccess: async () => {
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'group-verifications'],
      });
    },
  });
};
