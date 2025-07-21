import type { ContactInformationAction } from 'konfigyr/components/contact';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { Cloud, ArrowRight, Podcast } from 'lucide-react';
import { Button } from 'konfigyr/components/ui';
import Hero from './hero';
import Contact from './contact';
import Features from './features';

export default function LandingPage({ onContact } : { onContact: ContactInformationAction }) {
  const t = useTranslations('landing-page');

  return (
    <div>
      <section className="container py-18 md:py-30 lg:py-40">
        <Hero>
          <div className="flex items-center gap-4">
            <Button variant="secondary" className="h-14 px-12 text-lg" asChild>
              <Link href="/auth/authorize">
                {t('cta.join')} <ArrowRight className="h-5"/>
              </Link>
            </Button>
            <Button variant="ghost" className="h-14 px-12 text-lg" asChild>
              <Link href="#contact">
                {t('cta.contact')} <Podcast className="h-5"/>
              </Link>
            </Button>
          </div>
        </Hero>
      </section>

      <section className="container py-16">
        <Features />
      </section>

      <section id="contact" className="container py-16">
        <Contact action={onContact} />
      </section>

      <section className="container py-16">
        <div className="max-w-2xl mx-auto text-center space-y-8">
          <div className="space-y-4">
            <h2 className="text-3xl font-bold">
              {t('signup.title')}
            </h2>
            <p className="text-lg">
              {t('signup.subtitle')}
            </p>
          </div>

          <div className="flex flex-col items-center">
            <Button size="lg" asChild>
              <Link href="/auth/authorize">
                <Cloud/> Get started
              </Link>
            </Button>
          </div>

          <p className="text-gray-600 text-sm">
            {t('signup.footer')}
          </p>
        </div>
      </section>
    </div>
  );
}
