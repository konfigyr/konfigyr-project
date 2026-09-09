import { FormattedMessage, defineMessage } from 'react-intl';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

import type { MessageDescriptor } from 'react-intl';

interface Objection {
  question: MessageDescriptor;
  answer: MessageDescriptor;
}

const OBJECTIONS: Array<Objection> = [
  {
    question: defineMessage({
      defaultMessage: 'We already run HashiCorp Vault for secrets.',
      description: 'Question for the `HashiCorp Vault` objection',
    }),
    answer: defineMessage({
      defaultMessage: 'If Vault is doing pure secrets storage for you, Konfigyr replaces it outright, and adds what Vault has no concept of: validation against real Spring Boot property types, deprecation surfacing, and version-to-version schema tracking. If you\'re also using Vault for dynamic database credentials or PKI, that\'s outside what Konfigyr does today, and you\'d keep Vault for that piece.',
      description: 'Answer for the `HashiCorp Vault` objection',
    }),
  },
  {
    question: defineMessage({
      defaultMessage: 'We\'re not a Spring Boot shop.',
      description: 'Question for the `not a Spring Boot shop` objection',
    }),
    answer: defineMessage({
      defaultMessage: 'Spring Boot is our flagship integration, not a ceiling. Konfigyr is built on our own Artifactory schema registry, and spring-configuration-metadata.json is the first ingestion path we\'ve shipped, not the only one the architecture allows. We don\'t have a committed roadmap for other ecosystems yet, so if you need that today, Konfigyr isn\'t there yet either.',
      description: 'Answer for the `not a Spring Boot shop` objection',
    }),
  },
  {
    question: defineMessage({
      defaultMessage: 'We already use Doppler.',
      description: 'Question for the `Doppler` objection',
    }),
    answer: defineMessage({
      defaultMessage: 'Doppler is a solid, generic secrets tool for any language. Konfigyr\'s difference is depth on Spring Boot specifically: real property types pulled from your own build metadata, deprecation warnings, and schema tracking tied to artifact versions, not just key-value pairs with an environment label attached.',
      description: 'Answer for the `Doppler` objection',
    }),
  },
];

export function ObjectionHandling() {
  return (
    <Section>
      <SectionHeader className="max-w-[56ch] mx-auto">
        <SectionTitle className="text-3xl text-center">
          <FormattedMessage
            defaultMessage="Where Konfigyr fits next to what you already run."
            description="Title for the objection handling section"
          />
        </SectionTitle>
      </SectionHeader>
      <SectionContent className="gap-8">
        {OBJECTIONS.map((objection, index) => (
          <div key={index} className="grid gap-2">
            <p className="font-medium">
              &ldquo;<FormattedMessage {...objection.question} />&rdquo;
            </p>
            <p className="text-sm text-muted-foreground leading-relaxed">
              <FormattedMessage {...objection.answer} />
            </p>
          </div>
        ))}
      </SectionContent>
    </Section>
  );
}
