import { FormattedMessage, defineMessages } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { CallToActionLink } from '@konfigyr/components/cta';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

const messages = defineMessages({
  lead: {
    defaultMessage: 'We\'re taking on a small number of design partners.',
    description: 'The final call to action section lead',
  },
  subtitle: {
    defaultMessage: 'Konfigyr is pre-launch. There\'s no self-serve signup and no pricing yet. We\'re looking for a handful of Spring Boot teams willing to run it early, in exchange for direct input into what we build next. <link>Read more about where we are</link>.',
    description: 'The final call to action section subtitle, contains a link to the about page',
  },
});

export function FinalCta() {
  return (
    <Section variant="secondary">
      <SectionHeader className="max-w-[56ch] mx-auto">
        <SectionTitle className="text-3xl text-center">
          <FormattedMessage {...messages.lead} />
        </SectionTitle>
      </SectionHeader>
      <SectionContent className="text-muted-foreground leading-relaxed">
        <FormattedMessage
          {...messages.subtitle}
          values={{
            link: (chunks) => (
              <Link
                to="/about"
                key="link"
                className="underline underline-offset-4 hover:text-primary transition-colors"
              >
                {chunks}
              </Link>
            ),
          }}
          tagName="p"
        />

        <div className="text-center mt-8">
          <CallToActionLink size="lg" />
        </div>
      </SectionContent>
    </Section>
  );
}
