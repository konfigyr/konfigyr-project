import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import { NextIntlClientProvider } from 'next-intl';
import { getLocale } from 'next-intl/server';
import Layout from 'konfigyr/components/layout';
import './globals.css';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
    title: 'konfigyr.vault',
    description: 'Configuration made easy',
};

export default async function RootLayout({ children, }: { children: React.ReactNode }) {
    const locale = await getLocale();

    return (
        <html lang={locale}>
        <body>
        <NextIntlClientProvider locale={locale}>
            <Layout>
                {children}
            </Layout>
        </NextIntlClientProvider>
        </body>
        </html>
    );
}
