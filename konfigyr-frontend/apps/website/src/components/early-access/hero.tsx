import { FormattedMessage, defineMessages } from 'react-intl';
import {
  Section,
  SectionContent,
  SectionDescription,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Request Early Access',
    description: 'The early access hero section lead',
  },
  subtitle: {
    defaultMessage: 'Konfigyr is pre-launch. We\'re onboarding a small number of Spring Boot teams as design partners: tell us about your setup and we\'ll follow up directly.',
    description: 'The early access hero section subtitle',
  },
  expectations: {
    defaultMessage: 'There\'s no self-serve signup yet, and no pricing. This isn\'t a free trial with a credit card at the end of it. It\'s a request to work directly with us while we build. If it looks like a fit, we\'ll reach out to set up a call.',
    description: 'The early access hero section paragraph setting expectations about the design partner program',
  },
});

export function Hero() {
  return (
    <>
      <Section>
        <SectionHeader>
          <SectionTitle render={<h1/>}>
            <FormattedMessage {...messages.lead} />
          </SectionTitle>
          <SectionDescription>
            <FormattedMessage {...messages.subtitle} />
          </SectionDescription>
        </SectionHeader>
        <SectionContent>
          <p className="text-sm text-muted-foreground leading-relaxed">
            <FormattedMessage {...messages.expectations} />
          </p>
        </SectionContent>
      </Section>
    </>
  );
}
