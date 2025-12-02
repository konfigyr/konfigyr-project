import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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

export const useUpdateAccountName = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (name?: string): Promise<Account> => {
      return await request.patch('api/account', { json: { name } })
        .json();
    },
    onSuccess(account: Account) {
      client.setQueryData(accountKeys.getAccount(), account);
    },
  });
};

export const useUpdateAccountEmail = () => {
  return useMutation({
    mutationFn: async (email?: string): Promise<{ token: string }> => {
      return await request.post('api/account/email', { json: { email } })
        .json();
    },
  });
};

export const useConfirmAccountEmailChange = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ token, code }: { token: string, code: string }): Promise<Account> => {
      return await request.put('api/account/email', { json: { token, code } })
        .json();
    },
    onSuccess(account: Account) {
      client.setQueryData(accountKeys.getAccount(), account);
    },
  });
};
