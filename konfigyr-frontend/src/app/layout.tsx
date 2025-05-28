import type { Metadata } from 'next';
import { NextIntlClientProvider } from 'next-intl';
import { cookies } from 'next/headers';
import { getLocale } from 'next-intl/server';
import { getAccount} from 'konfigyr/services/authentication';
import { AccountContextProvider } from '../components/account';
import Layout from 'konfigyr/components/layout';
import './globals.css';

export const metadata: Metadata = {
  title: 'konfigyr.vault',
  description: 'Configuration made easy',
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const account = await getAccount(await cookies());
  const locale = await getLocale();

  return (
    <html lang={locale}>
      <body>
        <NextIntlClientProvider locale={locale}>
          <AccountContextProvider account={account}>
            <Layout>
              {children}
            </Layout>
          </AccountContextProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
