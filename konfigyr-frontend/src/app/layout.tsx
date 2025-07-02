import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { cookies } from 'next/headers';
import { getLocale } from 'next-intl/server';
import { getAccount, AuthenticationNotFoundError, type Account } from 'konfigyr/services/authentication';
import { AccountContextProvider } from '../components/account';
import Layout from 'konfigyr/components/layout';
import { Toaster } from 'konfigyr/components/ui';
import './globals.css';

export const metadata: Metadata = {
  title: {
    template: '%s | konfigyr.vault',
    default: 'konfigyr.vault',
  },
  description: 'Configuration made easy',
};

async function retrieveAccount(): Promise<Account | null> {
  const store = await cookies();

  try {
    return await getAccount(store);
  } catch(e) {
    if (e instanceof AuthenticationNotFoundError) {
      return null;
    }
    throw e;
  }
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const account = await retrieveAccount();
  const locale = await getLocale();

  return (
    <html lang={locale}>
      <body>
        <NextIntlClientProvider locale={locale}>
          <AccountContextProvider account={account}>
            <Layout>
              {children}
            </Layout>

            <Toaster />
          </AccountContextProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
