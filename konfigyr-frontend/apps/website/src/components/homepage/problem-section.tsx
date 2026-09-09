import { FormattedMessage, defineMessages } from 'react-intl';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

const messages = defineMessages({
  lead: {
    defaultMessage: 'You already know what this looks like.',
    description: 'The problem section lead',
  },
  drift: {
    defaultMessage: 'Config drifts between dev, staging, and prod because there\'s no single source of truth for what\'s actually deployed. Someone edits an <code>application.yml</code> by hand, or pastes a new value into whatever secrets tool you\'re running, and there\'s no validation: it either works or it breaks. When it breaks in production, you\'re grepping deploy logs to find out who changed what, and there\'s still no audit trail to point to the next time a compliance review asks the same question.',
    description: 'The problem section paragraph describing how configuration drifts between environments',
  },
  gap: {
    defaultMessage: 'The tools built to help don\'t close the gap. A generic secret store treats your configuration as an opaque key-value blob. It has no idea a property is deprecated, what type it expects, or whether it\'s even valid for the version of your service that\'s running. It can keep a value encrypted, but it can\'t tell you the value is wrong.',
    description: 'The problem section paragraph describing why generic secret stores fall short',
  },
});

export function ProblemSection() {
  return (
    <Section>
      <SectionHeader className="max-w-[56ch] mx-auto">
        <SectionTitle className="text-3xl text-center">
          <FormattedMessage {...messages.lead} />
        </SectionTitle>
      </SectionHeader>
      <SectionContent className="text-muted-foreground leading-relaxed">
        <FormattedMessage
          {...messages.drift}
          values={{
            code: (chunks) => (
              <code className="text-sm" key="code">
                {chunks}
              </code>
            ),
          }}
          tagName="p"
        />
        <FormattedMessage {...messages.gap} tagName="p"/>
      </SectionContent>
    </Section>
  );
}
