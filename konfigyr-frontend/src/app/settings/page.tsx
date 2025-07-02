import type { Metadata } from 'next';

import { useTranslations } from 'next-intl';
import { getTranslations } from 'next-intl/server';
import { Separator } from 'konfigyr/components/ui';
import {
  AccountEmailForm,
  AccountNameForm,
  AccountDeleteForm,
} from 'konfigyr/components/account';
import { deleteAccount } from './actions';

export async function generateMetadata({ params }: { params: { locale: string } }): Promise<Metadata> {
  const { locale } = params;
  const t = await getTranslations({ locale, namespace: 'account.form' });

  return {
    title: t('title'),
  };
}

export default function SettingsPage() {
  const t = useTranslations('account.form');

  return (
    <>
      <div className="container">
        <div className="w-full lg:w-1/2 mx-auto">
          <h1 className="text-3xl font-semibold mb-8">{t('title')}</h1>
        </div>
      </div>

      <Separator />

      <div className="container">
        <div className="w-full lg:w-1/2 mx-auto">
          <AccountEmailForm />
          <AccountNameForm />
          <AccountDeleteForm action={deleteAccount} />
        </div>
      </div>
    </>
  );
}
