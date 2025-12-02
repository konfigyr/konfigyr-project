import { queryOptions, useQuery } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { Account } from './types';

/**
 * Keys used to store the account in the query client.
 */
export const accountKeys = {
  getAccount: () => ['account'],
};

/**
 * Attempts to retrieve the currently authenticated user account from the Konfigyr API server.
 *
 * @returns TansStack query options to retrieve the account
 */
export const getAccountQuery = () => {
  return queryOptions({
    queryKey: accountKeys.getAccount(),
    queryFn: ({ signal }) => request.get('api/account', { signal }).json<Account>(),
    staleTime: Infinity,
  });
};

/**
 * Hook that retrieves the currently authenticated user account from the Konfigyr API server.
 */
export const useGetAccount = () => {
  return useQuery(getAccountQuery());
};
