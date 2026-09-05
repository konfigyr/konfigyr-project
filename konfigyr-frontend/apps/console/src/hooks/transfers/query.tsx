import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type {
  ArtifactOwnershipTransfer,
  ArtifactOwnershipTransferQuery,
  RequestTransferPayload,
} from './types';

export const transferKeys = {
  getTransfer: (namespace: string, id: string) => ['namespace', namespace, 'artifact-transfers', id],
  getIncomingTransfers: (namespace: string, query: ArtifactOwnershipTransferQuery) => ['namespace', namespace, 'artifact-transfers', 'incoming', query],
  getOutgoingTransfers: (namespace: string, query: ArtifactOwnershipTransferQuery) => ['namespace', namespace, 'artifact-transfers', 'outgoing', query],
};

export const getIncomingTransfers = (namespace: string, query: ArtifactOwnershipTransferQuery = {}) => {
  return queryOptions({
    queryKey: transferKeys.getIncomingTransfers(namespace, query),
    queryFn: async ({ signal }): Promise<PageResponse<ArtifactOwnershipTransfer>> => {
      return await request
        .get(`api/namespaces/${namespace}/artifact-transfers/incoming`, { signal, searchParams: { ...query } })
        .json();
    },
    placeholderData: previousData => previousData,
  });
};

export const getOutgoingTransfers = (namespace: string, query: ArtifactOwnershipTransferQuery = {}) => {
  return queryOptions({
    queryKey: transferKeys.getOutgoingTransfers(namespace, query),
    queryFn: async ({ signal }): Promise<PageResponse<ArtifactOwnershipTransfer>> => {
      return await request
        .get(`api/namespaces/${namespace}/artifact-transfers/outgoing`, { signal, searchParams: { ...query } })
        .json();
    },
    placeholderData: previousData => previousData,
  });
};

export const getTransfer = (namespace: string, id: string) => {
  return queryOptions({
    queryKey: transferKeys.getTransfer(namespace, id),
    queryFn: async ({ signal }): Promise<ArtifactOwnershipTransfer> => {
      return await request
        .get(`api/namespaces/${namespace}/artifact-transfers/${id}`, { signal })
        .json();
    },
  });
};

export const useGetIncomingTransfers = (namespace: string, query?: ArtifactOwnershipTransferQuery) => {
  return useQuery(getIncomingTransfers(namespace, query));
};

export const useGetOutgoingTransfers = (namespace: string, query?: ArtifactOwnershipTransferQuery) => {
  return useQuery(getOutgoingTransfers(namespace, query));
};

export const useGetTransfer = (namespace: string, id: string) => {
  return useQuery(getTransfer(namespace, id));
};

export const useRequestTransfer = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (payload: RequestTransferPayload): Promise<ArtifactOwnershipTransfer> => {
      return request.post(`api/namespaces/${namespace}/artifact-transfers`, {
        json: payload,
      }).json<ArtifactOwnershipTransfer>();
    },
    onSuccess: async (transfer) => {
      client.setQueryData(transferKeys.getTransfer(namespace, transfer.id), transfer);
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'artifact-transfers'],
      });
    },
  });
};

export const useAcceptTransfer = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (id: string): Promise<ArtifactOwnershipTransfer> => {
      return request.post(`api/namespaces/${namespace}/artifact-transfers/${id}/accept`).json<ArtifactOwnershipTransfer>();
    },
    onSuccess: async (transfer) => {
      client.setQueryData(transferKeys.getTransfer(namespace, transfer.id), transfer);
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'artifact-transfers'],
      });
    },
  });
};

export const useRejectTransfer = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (id: string): Promise<ArtifactOwnershipTransfer> => {
      return request.post(`api/namespaces/${namespace}/artifact-transfers/${id}/reject`).json<ArtifactOwnershipTransfer>();
    },
    onSuccess: async (transfer) => {
      client.setQueryData(transferKeys.getTransfer(namespace, transfer.id), transfer);
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'artifact-transfers'],
      });
    },
  });
};

export const useCancelTransfer = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (id: string): Promise<ArtifactOwnershipTransfer> => {
      return request.delete(`api/namespaces/${namespace}/artifact-transfers/${id}`).json<ArtifactOwnershipTransfer>();
    },
    onSuccess: async (transfer) => {
      client.setQueryData(transferKeys.getTransfer(namespace, transfer.id), transfer);
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'artifact-transfers'],
      });
    },
  });
};
