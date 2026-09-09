import { FormattedMessage, defineMessages } from 'react-intl';
import { CallToActionLink } from '@konfigyr/components/cta';
import {
  Section,
  SectionContent,
  SectionDescription,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Configuration management for Spring Boot.',
    description: 'The hero section lead',
  },
  subtitle: {
    defaultMessage: 'Not a generic secrets store with a UI on top. Konfigyr reads your build\'s own property metadata and validates every value against it before it reaches production.',
    description: 'The hero section subtitle',
  },
});

export function Hero() {
  return (
    <Section className="sm:py-28 text-center">
      <SectionHeader className="max-w-[72ch] mx-auto">
        <SectionTitle render={<h1/>} className="text-4xl sm:text-5xl">
          <FormattedMessage {...messages.lead} />
        </SectionTitle>
        <SectionDescription className="sm:text-xl text-balance my-8 mx-auto">
          <FormattedMessage {...messages.subtitle} />
        </SectionDescription>
      </SectionHeader>
      <SectionContent>
        <div>
          <CallToActionLink size="lg" className="h-12 px-10" />
        </div>
      </SectionContent>
    </Section>
  );
}
