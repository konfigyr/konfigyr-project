import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { CollectionResponse, PageResponse } from '@konfigyr/hooks/hateoas/types';
import type {
  CreateKeyset,
  Key,
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
  getKeysetKeys: (namespace: string, keyset: string) => ['kms', namespace, 'keyset', keyset, 'keys'],
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
      const response = await request.get(`api/namespaces/${namespace}/kms`, {
        signal, searchParams: { ...query },
      }).json<PageResponse<Keyset>>();

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
 * Attempts to retrieve a single Keyset by identifier from the Konfigyr API server for the given namespace.
 *
 * @returns TansStack query options to retrieve the keyset
 */
export const getKeysetQuery = (namespace: string, keyset: string) => {
  return queryOptions({
    queryKey: kmsKeys.getKeyset(namespace, keyset),
    queryFn: async ({ signal }) => request
      .get(`api/namespaces/${namespace}/kms/${keyset}`, { signal })
      .json<Keyset>(),
  });
};

/**
 * Attempts to retrieve a collection of keys that belong to a Keyset from the Konfigyr API server
 * for the given namespace.
 *
 * @returns TansStack query options to retrieve the keyset
 */
export const getKeysetKeysQuery = (namespace: string, keyset: string) => {
  return queryOptions({
    queryKey: kmsKeys.getKeysetKeys(namespace, keyset),
    queryFn: async ({ signal }) => request
      .get(`api/namespaces/${namespace}/kms/${keyset}/keys`, { signal })
      .json<CollectionResponse<Key>>()
      .then(response => response.data),
  });
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
    mutationFn: (payload?: unknown): Promise<Keyset> => {
      return request.put(`api/namespaces/${namespace}/kms/${keyset.id}/${operation}`, { json: payload })
        .json<Keyset>();
    },
    onSuccess: (result: Keyset) => {
      keyset.state = result.state;
      client.setQueryData(kmsKeys.getKeyset(namespace, keyset.id), result);
      client.removeQueries({
        queryKey: kmsKeys.getKeysetKeys(namespace, keyset.id),
      });
    },
  });
};

const useKeyLifecycleOperation = (namespace: string, keyset: Keyset, operation: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (key: Key): Promise<Key> => {
      return request.put(`api/namespaces/${namespace}/kms/${keyset.id}/keys/${key.id}/${operation}`).json<Key>();
    },
    onSuccess: () => {
      client.removeQueries({
        queryKey: kmsKeys.getKeysetKeys(namespace, keyset.id),
      });
    },
  });
};

/**
 * Hook that rotates the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useRotateKeyset = (namespace: string, keyset: Keyset) => {
  return useKeysetLifecycleOperation(namespace, keyset, 'rotate');
};

/**
 * Hook that deactivates, or disables, a Key within the given Keyset in the Konfigyr API server
 * for the given namespace.
 */
export const useDisableKey = (namespace: string, keyset: Keyset) => {
  return useKeyLifecycleOperation(namespace, keyset, 'deactivate');
};

/**
 * Hook that reactivates a Key within the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useReactivateKey = (namespace: string, keyset: Keyset) => {
  return useKeyLifecycleOperation(namespace, keyset, 'reactivate');
};

/**
 * Hook that reactivates a Key within the given Keyset in the Konfigyr API server for the given namespace.
 */
export const useCompromiseKey = (namespace: string, keyset: Keyset) => {
  return useKeyLifecycleOperation(namespace, keyset, 'compromised');
};

/**
 * Hook that restores a Key that was scheduled for destruction within the given Keyset in the
 * Konfigyr API server for the given namespace.
 */
export const useRestoreKey = (namespace: string, keyset: Keyset) => {
  return useKeyLifecycleOperation(namespace, keyset, 'restore');
};

/**
 * Hook that schedules destruction of a Key within the given Keyset in the Konfigyr API server
 * for the given namespace.
 */
export const useDestroyKey = (namespace: string, keyset: Keyset) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (key: Key): Promise<Key> => {
      return request.delete(`api/namespaces/${namespace}/kms/${keyset.id}/keys/${key.id}`).json<Key>();
    },
    onSuccess: () => {
      client.removeQueries({
        queryKey: kmsKeys.getKeysetKeys(namespace, keyset.id),
      });
    },
  });
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
    onSuccess: (result: Keyset) => {
      keyset.state = result.state;

      client.removeQueries({
        queryKey: kmsKeys.getKeyset(namespace, keyset.id),
      });
    },
  });
};
