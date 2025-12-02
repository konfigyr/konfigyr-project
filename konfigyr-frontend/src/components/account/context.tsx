'use client';

import { useEffect, useState } from 'react';
import { FormattedMessage, KonfigyrLeadMessage, KonfigyrTitleMessage } from '@konfigyr/components/messages';
import { AccountContext, useGetAccount } from '@konfigyr/hooks';

import type { ReactNode } from 'react';

function AccountLoader() {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setWidth((prev) => {
        // Stop at 95% until completion
        if (prev >= 95) return prev;

        // Slower progress as we get higher (realistic loading)
        const increment = Math.random() * (100 - prev) * 0.1;
        const slowdown = prev > 70 ? 0.3 : prev > 50 ? 0.6 : 1;

        return Math.min(prev + increment * slowdown, 95);
      });
    }, 600);

    return () => clearInterval(interval);
  });

  return (
    <div className="h-screen w-screen gap-8 flex flex-col items-center justify-center text-center">
      <div className="space-y-2">
        <h1 className="text-5xl font-medium leading-snug">
          <KonfigyrTitleMessage />
        </h1>
        <p className="text-2xl">
          <KonfigyrLeadMessage />
        </p>
      </div>

      <div className="relative bg-gray-200 h-[2px] w-[18rem] z-50 pointer-events-none rounded-full">
        <div
          className="h-full bg-secondary transition-all duration-300 ease-out shadow-sm"
          style={{ width: `${width}%` }}
        />
      </div>

      <div className="space-y-1">
        <p className="font-medium">
          <FormattedMessage
            id="account.loader.message"
            defaultMessage="Loading your account information..."
            description="Message shown while the account is being retrieved from the Konfigyr API server"
          />
        </p>
        <p className="text-muted-foreground text-sm">
          <FormattedMessage
            id="account.loader.description"
            defaultMessage="This may take only a moment, please be patient."
            description="Description of the account loading process"
          />
        </p>
      </div>
    </div>
  );
}

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
    return (<AccountLoader />);
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
