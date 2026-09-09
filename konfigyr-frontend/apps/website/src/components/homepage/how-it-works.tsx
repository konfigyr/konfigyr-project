import { FormattedMessage, defineMessage } from 'react-intl';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

import type { MessageDescriptor } from 'react-intl';

interface Step {
  title: MessageDescriptor;
  description: MessageDescriptor;
}

const STEPS: Array<Step> = [
  {
    title: defineMessage({
      defaultMessage: 'Publish your metadata.',
      description: 'Title for the `Publish your metadata` how it works card',
    }),
    description: defineMessage({
      defaultMessage: 'Add the Konfigyr Gradle or Maven plugin. It pushes your spring-configuration-metadata.json to your namespace\'s Artifactory on every build, so you never have to author a schema by hand.',
      description: 'Description for the `Publish your metadata` how it works card',
    }),
  },
  {
    title: defineMessage({
      defaultMessage: 'Manage config per profile.',
      description: 'Title for the `Manage config per profile` how it works card',
    }),
    description: defineMessage({
      defaultMessage: 'Set values for dev, staging, and prod in a vault scoped to your service, validated against the schema for that exact artifact version.',
      description: 'Description for the `Manage config per profile` how it works card',
    }),
  },
  {
    title: defineMessage({
      defaultMessage: 'Review what needs review.',
      description: 'Title for the `Review what needs review` how it works card',
    }),
    description: defineMessage({
      defaultMessage: 'Changes to protected profiles go through a change request before they merge. Unprotected profiles apply directly.',
      description: 'Description for the `Review what needs review` how it works card',
    }),
  },
  {
    title: defineMessage({
      defaultMessage: 'Trace every change.',
      description: 'Title for the `Trace every change` how it works card',
    }),
    description: defineMessage({
      defaultMessage: 'Every value, every profile, and every approval lands in the same audit trail, tied to identity and always on.',
      description: 'Description for the `Trace every change` how it works card',
    }),
  },
];

export function HowItWorks() {
  return (
    <Section className="container max-w-4xl mx-auto px-4 py-16 grid gap-10">
      <SectionHeader className="max-w-[56ch] mx-auto">
        <SectionTitle className="text-3xl text-center">
          <FormattedMessage
            defaultMessage="From build to production, in four steps."
            description="Title for the how it works section"
          />
        </SectionTitle>
      </SectionHeader>
      <SectionContent>
        <ol className="grid gap-8 sm:grid-cols-2">
          {STEPS.map((step, index) => (
            <li key={index} className="flex gap-4">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground font-heading font-semibold">
                {index + 1}
              </span>
              <div className="grid gap-1">
                <p className="font-medium">
                  <FormattedMessage {...step.title} />
                </p>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  <FormattedMessage {...step.description} />
                </p>
              </div>
            </li>
          ))}
        </ol>
      </SectionContent>
    </Section>
  );
}
