import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { GroupVerification, GroupVerificationQuery } from './types';

export const groupVerificationKeys = {
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

export const useGetGroupVerifications = (namespace: string, query?: GroupVerificationQuery) => {
  return useQuery(getGroupVerifications(namespace, query));
};

export const useRevokeGroupVerification = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (groupId: string) => {
      await request.delete(`api/namespaces/${namespace}/group-verifications/${groupId}`);
    },
    onSuccess: () => {
      client.invalidateQueries({
        queryKey: ['namespace', namespace, 'group-verifications'],
      });
    },
  });
};
