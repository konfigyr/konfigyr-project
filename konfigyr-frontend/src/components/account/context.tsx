'use client';

import type { ReactNode, Dispatch } from 'react';
import type { Account } from 'konfigyr/services/authentication';
import { createContext, useContext, useState } from 'react';

export const AccountContext = createContext<{
  account: Account | null | undefined,
  setAccount: Dispatch<Account | null | undefined>
}>({
  account: null,
  setAccount: () => {},
});

export function useAccount(): [Account | null, Dispatch<Account | null>] {
  const ctx = useContext(AccountContext);
  return [ctx.account || null, ctx.setAccount];
}

export type AccountContextProviderProps = {
  account?: Account | null,
  children: ReactNode,
};

export function AccountContextProvider({ account, children }: AccountContextProviderProps) {
  const [value, setAccount] = useState(account);
  const ctx = { account: account == null ? null : value, setAccount };

  return (
    <AccountContext.Provider value={ctx}>
      {children}
    </AccountContext.Provider>
  );
}
