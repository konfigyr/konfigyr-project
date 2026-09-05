import { createContext, useContext } from 'react';
import type { Account, Namespace } from '@konfigyr/hooks/types';

export const AccountContext = createContext<{ account: Account, memberships: Array<Namespace> } | null>(null);

export const useAccountContext = () => {
  const context = useContext(AccountContext);

  if (context === null) {
    throw new Error('Account context is not available. Please make sure that the user is authenticated.');
  }

  return context;
};

/**
 * Hook that returns the currently authenticated user account. If the user account is not available,
 * it could be because the user is not authenticated or setup of the account has not been completed yet
 * in the root route.
 *
 * @returns {Account} the currently authenticated user account
 */
export const useAccount = (): Account => {
  const { account } = useAccountContext();
  return account;
};
