'use client';

import type { ReactNode } from 'react';
import type { Account } from 'konfigyr/services/authentication';
import { createContext, useContext } from 'react';

export const AccountContext = createContext<Account | null>(null);

export function useAccount(): Account | null {
  return useContext(AccountContext);
}

export type AccountContextProviderProps = {
  account?: Account | null,
  children: ReactNode,
};

export function AccountContextProvider({ account, children }: AccountContextProviderProps) {
  return (
    <AccountContext.Provider value={account || null}>
      {children}
    </AccountContext.Provider>
  );
}
