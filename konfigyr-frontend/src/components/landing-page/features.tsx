import { useTranslations } from 'next-intl';
import { Code, GitBranch, Lock, Shield, type LucideIcon } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from 'konfigyr/components/ui';

interface FeatureSettings {
  type: string,
  icon: LucideIcon,
}

const FEATURES: FeatureSettings[] = [
  {
    type: 'security',
    icon: Shield,
  },
  {
    type: 'environments',
    icon: GitBranch,
  },
  {
    type: 'integration',
    icon: Code,
  },
  {
    type: 'audit',
    icon: Lock,
  },
];

function FeatureCard({ type, icon: FeatureIcon }: FeatureSettings) {
  const t = useTranslations(`landing-page.features.${type}`);

  return (
    <Card className="shadow-xs hover:shadow transition-shadow">
      <CardHeader>
        <FeatureIcon className="h-8 w-8 text-secondary mb-2"/>
        <CardTitle>{t('title')}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-slate-600">
          {t('description')}
        </p>
      </CardContent>
    </Card>
  );
}

export default function Features() {
  const t = useTranslations('landing-page.features');

  return (
    <div className="px-6 md:px-12 lg:px-16 space-y-8">
      <div className="text-center space-y-4">
        <h2 className="text-3xl font-bold">
          {t('title')}
        </h2>
        <p className="text-slate-600 text-lg">
          {t('subtitle')}
        </p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {FEATURES.map(feature => (
          <FeatureCard key={feature.type} {...feature} />
        ))}
      </div>
    </div>
  );
}
