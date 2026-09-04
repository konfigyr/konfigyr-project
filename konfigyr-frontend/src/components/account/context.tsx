'use client';

import { ErrorState } from '@konfigyr/components/error';
import { FormattedMessage, KonfigyrLeadMessage, KonfigyrTitleMessage } from '@konfigyr/components/messages';
import { ProgressLoader } from '@konfigyr/components/ui/loader';
import LogoImage from '@konfigyr/public/logo.svg';
import { AccountContext, useGetAccount, useGetNamespaces } from '@konfigyr/hooks';

import type { ReactNode } from 'react';

function AccountLoader() {
  return (
    <div className="h-screen w-screen gap-8 flex flex-col items-center justify-center text-center">
      <div className="space-y-2">
        <h1 className="text-5xl font-medium leading-snug flex items-center justify-center gap-2">
          <LogoImage className="size-9" />
          <KonfigyrTitleMessage />
        </h1>
        <p className="text-2xl">
          <KonfigyrLeadMessage />
        </p>
      </div>

      <ProgressLoader />

      <div className="space-y-1">
        <p className="font-medium">
          <FormattedMessage
            defaultMessage="Loading your account information..."
            description="Message shown while the account is being retrieved from the Konfigyr API server"
          />
        </p>
        <p className="text-muted-foreground text-sm">
          <FormattedMessage
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
  const account = useGetAccount();
  const namespaces = useGetNamespaces();

  if (account.isPending || namespaces.isPending) {
    return (<AccountLoader />);
  }

  if (account.isError) {
    return (
      <ErrorState error={account.error} />
    );
  }

  if (namespaces.isError) {
    return (
      <ErrorState error={namespaces.error} />
    );
  }

  return (
    <AccountContext.Provider value={{ account: account.data, memberships: namespaces.data }}>
      {children}
    </AccountContext.Provider>
  );
};
