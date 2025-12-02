import { createContext, useContext } from 'react';
import type { Account } from '@konfigyr/hooks/types';

export const AccountContext = createContext<Account | null>(null);

export const useAccountContext = () => useContext(AccountContext);

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
