import { queryOptions, useQuery } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';
import { useAccountContext } from './context';

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
    queryFn: () => request.get('api/account').json<Account>(),
    staleTime: Infinity,
  });
};

/**
 * Hook that retrieves the currently authenticated user account from the Konfigyr API server.
 */
export const useGetAccount = () => {
  return useQuery(getAccountQuery());
};

/**
 * Hook that returns the currently authenticated user account. If the user account is not available,
 * it could be because the user is not authenticated or setup of the account has not been completed yet
 * in the root route.
 *
 * @returns {Account} the currently authenticated user account
 */
export const useAccount = () => {
  const account = useAccountContext();

  if (account === null) {
    throw new Error('Account is not available. Please make sure that the user is authenticated.');
  }

  return account;
};
