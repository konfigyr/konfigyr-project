import { queryOptions, useQuery } from '@tanstack/react-query';
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
