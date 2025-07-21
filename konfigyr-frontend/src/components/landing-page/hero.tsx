import { useTranslations } from 'next-intl';

export default function Hero({ children }: { children?: React.ReactNode }) {
  const t = useTranslations('landing-page.hero');

  return (
    <div className="flex flex-col items-center space-y-8 text-center">
      <div className="space-y-4 max-w-4xl">
        <h1 className="text-3xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
          {t('title')}
        </h1>
        <div className="mx-auto max-w-[700px] text-slate-600 md:text-lg lg:text-xl leading-relaxed">
          <p className="mb-4">
            {t('subtitle')}
          </p>
          <p className="font-medium">
            {t('lead')}
          </p>
        </div>
      </div>

      {children}
    </div>
  );
}
