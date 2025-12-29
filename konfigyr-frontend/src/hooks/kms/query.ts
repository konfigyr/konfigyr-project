import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type {
  CreateKeyset,
  Keyset,
  KeysetOperation,
  KeysetOperationRequests,
  KeysetOperationResponses,
  KeysetSearchQuery,
} from '@konfigyr/hooks/kms/types';

/**
 * Keys used to store the Keysets in the query client.
 */
export const kmsKeys = {
  getKeysets: (namespace: string, query: KeysetSearchQuery) => ['kms', namespace, 'keysets', query],
  getKeyset: (namespace: string, keyset: string) => ['kms', namespace, 'keyset', keyset],
};


/**
 * Attempts to retrieve the Keysets from the Konfigyr API server for the given namespace.
 *
 * @returns TansStack query options to retrieve the keysets
 */
export const getKeysetsQuery = (namespace: string, query: KeysetSearchQuery = {}) => {
  return queryOptions({
    queryKey: kmsKeys.getKeysets(namespace, query),
    queryFn: async ({ signal }) => {
      await new Promise(resolve => setTimeout(resolve, 1000));

      const response = await request.get(`api/namespaces/${namespace}/kms`, {
        signal, searchParams: query,
      }).json<{ data: Array<Keyset> }>();

      return response.data;
    },
  });
};

/**
 * Hook that retrieves the Keysets from the Konfigyr API server for the given namespace.
 */
export const useGetKeysets = (namespace: string, query: KeysetSearchQuery = {}) => {
  return useQuery(getKeysetsQuery(namespace, query));
};

/**
 * Hook that creates a new Keyset in the Konfigyr API server for the given namespace.
 */
export const useCreateKeyset = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateKeyset): Promise<Keyset> => {
      return request.post(`api/namespaces/${namespace}/kms`, { json: payload })
        .json<Keyset>();
    },
    onSuccess(result: Keyset) {
      client.setQueryData(kmsKeys.getKeyset(namespace, result.id), result);
    },
  });
};

/**
 * Hook that performs a Keyset operation in the Konfigyr API server for the given namespace.
 */
export const useKeysetOperation = <
  TOperation extends KeysetOperation,
  TRequest extends KeysetOperationRequests[TOperation],
  TResponse extends KeysetOperationResponses[TOperation],
>(
  namespace: string, keyset: string, operation: TOperation,
) => {
  return useMutation({
    mutationFn: (payload: TRequest): Promise<TResponse> => {
      return request.post(`api/namespaces/${namespace}/kms/${keyset}/${operation}`, { json: payload })
        .json<TResponse>();
    },
  });
};

const useKeysetLifecycleOperation = (namespace: string, keyset: Keyset, operation: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (): Promise<Keyset> => {
      return request.put(`api/namespaces/${namespace}/kms/${keyset.id}/${operation}`).json<Keyset>();
    },
    onSuccess(result: Keyset) {
      keyset.state = result.state;
      client.setQueryData(kmsKeys.getKeyset(namespace, keyset.id), result);
    },
  });
};

/**
 * Hook that reactivates the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useReactivateKeyset = (namespace: string, keyset: Keyset) => {
  return useKeysetLifecycleOperation(namespace, keyset, 'reactivate');
};

/**
 * Hook that rotates the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useRotateKeyset = (namespace: string, keyset: Keyset) => {
  return useKeysetLifecycleOperation(namespace, keyset, 'rotate');
};

/**
 * Hook that disables the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useDisableKeyset = (namespace: string, keyset: Keyset) => {
  return useKeysetLifecycleOperation(namespace, keyset, 'disable');
};

/**
 * Hook that destroys the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useDestroyKeyset = (namespace: string, keyset: Keyset) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (): Promise<Keyset> => {
      return request.delete(`api/namespaces/${namespace}/kms/${keyset.id}`).json<Keyset>();
    },
    onSuccess(result: Keyset) {
      keyset.state = result.state;
      client.setQueryData(kmsKeys.getKeyset(namespace, keyset.id), result);
    },
  });
};
