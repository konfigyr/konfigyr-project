'use client';

import { AccountContext } from '@konfigyr/hooks/account/context';
import { useGetAccount } from '@konfigyr/hooks';

import type { ReactNode } from 'react';

/**
 * The `<AccountProvider>` component provides the account context to the entire application. It would attempt
 * to retrieve the currently authenticated user account from the Konfigyr API server. During this process,
 * it would render the fullscreen loading indicator until the account is retrieved.
 *
 * In case of an error, it would render the error screen instead, blocking any further rendering of the application.
 *
 * This component should be rendered at the root of the application to make the account context available
 * for the rest of the application via React Context API. To load the account, please use the `useAccount` hook
 * from the `@konfigyr/hooks/account` package.
 *
 * @param children the children elements to be rendered
 */
export const AccountProvider = ({ children }: { children: ReactNode }) => {
  const { data: account, isPending, isError, error } = useGetAccount();

  if (isPending) {
    return <div>Loading...</div>;
  }

  if (isError) {
    return (
      <div>
        <p>Failed to retrieve account</p>
        <p>{error.message}</p>
      </div>
    );
  }

  return (
    <AccountContext.Provider value={account}>
      {children}
    </AccountContext.Provider>
  );
};
