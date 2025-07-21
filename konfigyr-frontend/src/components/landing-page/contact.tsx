import { useTranslations } from 'next-intl';
import { ContactForm } from 'konfigyr/components/contact';
import { Card, CardContent, CardHeader, CardTitle, Separator } from 'konfigyr/components/ui';

const QUESTION_TYPES = ['beta', 'pricing', 'config-migration'];
const CONTACT_LINK = (
  <a href="mailto:contact@konfigyr.com" target="_blank" className="font-mono">
    contact@konfigyr.com
  </a>
);

function Question({ type }: { type: string }) {
  const t = useTranslations(`landing-page.contact.faq.${type}`);

  return (
    <div>
      <h4 className="font-medium text-slate-800">
        {t('question')}
      </h4>
      <p className="text-slate-600 text-sm">
        {t('answer')}
      </p>
    </div>
  )
}

function FormCard({ action }: React.ComponentProps<typeof ContactForm>) {
  const t = useTranslations('contact');

  return (
    <Card className="border-slate-200">
      <CardHeader>
        <CardTitle className="text-xl">
          {t('title')}
        </CardTitle>
        <p className="text-slate-600">
          {t('subtitle')}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <ContactForm action={action}/>
      </CardContent>
    </Card>
  );
}

export default function Contact({ action } : React.ComponentProps<typeof ContactForm>) {
  const t = useTranslations('landing-page.contact');

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div className="text-center space-y-4">
        <h2 className="text-3xl font-bold">
          {t('title')}
        </h2>
        <p className="text-slate-600 text-lg">
          {t('subtitle')}
        </p>
      </div>

      <div className="grid md:grid-cols-2 gap-12 items-start">
        <FormCard action={action} />

        <div className="space-y-8">
          <div>
            <h3 className="text-xl font-semibold text-slate-900 mb-4">
              {t('faq.title')}
            </h3>
            <div className="space-y-4">
              {QUESTION_TYPES.map(type => (
                <Question key={type} type={type} />
              ))}
            </div>
          </div>

          <Separator/>

          <div>
            <h4 className="text-xl font-semibold text-slate-700 mb-4">
              {t('direct.title')}
            </h4>
            <p className="text-slate-600 mb-3">
              {t.rich('direct.instructions', { link: () => CONTACT_LINK })}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
