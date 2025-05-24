import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { Button } from 'konfigyr/components/ui';
import ServiceLabel from 'konfigyr/components/service-label';

export default function Header() {
  const t = useTranslations('authentication');

  return (
    <header id="header" className="border-b">
      <div className="container px-4 flex h-14 items-center gap-4">
        <Link href="/" title="Konfigyr Vault" className="text-2xl">
          <ServiceLabel/>
        </Link>

        <div className="flex flex-1 items-center justify-end">
          <Button variant="link" asChild>
            <Link href="/auth/authorize">
              {t('login')}
            </Link>
          </Button>
        </div>
      </div>
    </header>
  );
}
