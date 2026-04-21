import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';

import type { CollectionResponse, PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type {
  ChangeRequest,
  ChangeRequestChange,
  ChangeRequestHistory,
  ChangeRequestQuery,
  SubmitChangeRequestReview,
  VaultRevisionInformation,
} from '@konfigyr/hooks/vault/types';

/**
 * Keys used to store the change requests in the query client.
 */
export const changeRequestKeys = {
  getChangeRequests: (namespace: Namespace, service: Service, query: ChangeRequestQuery) => [
    'namespace', namespace.slug, 'services', service.slug, 'change-requests', query,
  ],
  getChangeRequest: (namespace: Namespace, service: Service, number: number | string) => [
    'namespace', namespace.slug, 'services', service.slug, 'change-request', String(number),
  ],
  getChangeRequestChanges: (namespace: Namespace, service: Service, number: number | string) => [
    'namespace', namespace.slug, 'services', service.slug, 'change-request', String(number), 'changes',
  ],
  getChangeRequestHistory: (namespace: Namespace, service: Service, number: number | string) => [
    'namespace', namespace.slug, 'services', service.slug, 'change-request', String(number), 'history',
  ],
};

export const getChangeRequestsQuery = (namespace: Namespace, service: Service, query: ChangeRequestQuery) => {
  return queryOptions({
    queryKey: changeRequestKeys.getChangeRequests(namespace, service, query),
    queryFn: async ({ signal }): Promise<PageResponse<ChangeRequest>> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes`;

      return request.get(uri, { searchParams: query, signal })
        .json<PageResponse<ChangeRequest>>();
    },
    placeholderData: previousData => previousData,
  });
};

export const useGetChangeRequests = (namespace: Namespace, service: Service, query: ChangeRequestQuery) => {
  return useQuery(getChangeRequestsQuery(namespace, service, query));
};

export const getChangeRequestQuery = (namespace: Namespace, service: Service, number: string | number) => {
  return queryOptions({
    queryKey: changeRequestKeys.getChangeRequest(namespace, service, number),
    queryFn: async ({ signal }): Promise<ChangeRequest> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${number}`;
      return request.get(uri, { signal }).json<ChangeRequest>();
    },
  });
};

export const useGetChangeRequest = (namespace: Namespace, service: Service, number: string | number) => {
  return useQuery(getChangeRequestQuery(namespace, service, number));
};

export const getChangeRequestHistoryQuery = (namespace: Namespace, service: Service, number: string | number) => {
  return queryOptions({
    queryKey: changeRequestKeys.getChangeRequestHistory(namespace, service, number),
    queryFn: async ({ signal }): Promise<CollectionResponse<ChangeRequestHistory>> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${number}/history`;
      return request.get(uri, { signal }).json<CollectionResponse<ChangeRequestHistory>>();
    },
  });
};

export const useGetChangeRequestHistory = (namespace: Namespace, service: Service, number: string | number) => {
  return useQuery(getChangeRequestHistoryQuery(namespace, service, number));
};

export const getChangeRequestChangesQuery = (namespace: Namespace, service: Service, number: string | number) => {
  return queryOptions({
    queryKey: changeRequestKeys.getChangeRequestChanges(namespace, service, number),
    queryFn: async ({ signal }): Promise<CollectionResponse<ChangeRequestChange>> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${number}/changes`;
      return request.get(uri, { signal }).json<CollectionResponse<ChangeRequestChange>>();
    },
  });
};

export const useGetChangeRequestChanges = (namespace: Namespace, service: Service, number: string | number) => {
  return useQuery(getChangeRequestChangesQuery(namespace, service, number));
};

export const useUpdateChangeRequest = (namespace: Namespace, service: Service, changeRequest: ChangeRequest) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: ({ subject, description }: { subject?: string, description?: string }): Promise<ChangeRequest> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${changeRequest.number}`;

      return request.put(uri, { json: { subject, description } }).json<ChangeRequest>();
    },
    onSuccess: async (result: ChangeRequest) => {
      client.setQueryData(changeRequestKeys.getChangeRequest(namespace, service, changeRequest.number), result);
      await client.invalidateQueries({
        queryKey: changeRequestKeys.getChangeRequestHistory(namespace, service, changeRequest.number),
      });
    },
  });
};

export const useReviewChangeRequest = (namespace: Namespace, service: Service, changeRequest: ChangeRequest) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (review: SubmitChangeRequestReview): Promise<ChangeRequest> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${changeRequest.number}/review`;

      return request.post(uri, { json: review }).json<ChangeRequest>();
    },
    onSuccess: async (result: ChangeRequest) => {
      client.setQueryData(changeRequestKeys.getChangeRequest(namespace, service, changeRequest.number), result);
      await client.invalidateQueries({
        queryKey: changeRequestKeys.getChangeRequestHistory(namespace, service, changeRequest.number),
      });
    },
  });
};

export const useMergeChangeRequest = (namespace: Namespace, service: Service, changeRequest: ChangeRequest) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (): Promise<VaultRevisionInformation> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${changeRequest.number}/merge`;
      return request.post(uri).json<VaultRevisionInformation>();
    },
    onSuccess: async () => {
      client.setQueryData(changeRequestKeys.getChangeRequest(namespace, service, changeRequest.number), {
        ...changeRequest,
        state: ChangeRequestState.MERGED,
      });

      await client.invalidateQueries({
        queryKey: changeRequestKeys.getChangeRequestHistory(namespace, service, changeRequest.number),
      });
    },
  });
};

export const useDiscardChangeRequest = (namespace: Namespace, service: Service, changeRequest: ChangeRequest) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (): Promise<ChangeRequest> => {
      const uri = `api/namespaces/${namespace.slug}/services/${service.slug}/changes/${changeRequest.number}`;
      return request.delete(uri).json<ChangeRequest>();
    },
    onSuccess(result: ChangeRequest) {
      client.setQueryData(changeRequestKeys.getChangeRequest(namespace, service, changeRequest.number), result);
    },
  });
};
