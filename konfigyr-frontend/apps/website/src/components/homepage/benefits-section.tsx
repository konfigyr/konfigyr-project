import { FormattedMessage, defineMessage } from 'react-intl';
import { Card, CardContent } from '@konfigyr/ui/components/card';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';
import { ScreenshotPlaceholder } from '@konfigyr/components/homepage/screenshot-placeholder';

import type { MessageDescriptor } from 'react-intl';

interface Benefit {
  title: MessageDescriptor;
  description: MessageDescriptor;
}

interface BenefitPillar {
  title: MessageDescriptor;
  benefits: Array<Benefit>;
  screenshot: MessageDescriptor;
}

const PILLARS: Array<BenefitPillar> = [
  {
    title: defineMessage({
      defaultMessage: 'Configuration, done right',
      description: 'Title for the `Configuration, done right` benefits pillar',
    }),
    benefits: [
      {
        title: defineMessage({
          defaultMessage: 'Type-safe, before it ships.',
          description: 'Title for the `Type-safe, before it ships` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Every value is checked against the real JSON Schema for that exact artifact version, catching a bad type or an invalid value before it\'s applied, not after a deploy fails.',
          description: 'Description for the `Type-safe, before it ships` benefit card',
        }),
      },
      {
        title: defineMessage({
          defaultMessage: 'A full audit trail, by default.',
          description: 'Title for the `A full audit trail, by default` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Every change, to every namespace and every profile, is tied to an identity and logged. There\'s no separate audit tool to bolt on and no reconstructing "who changed this" from deploy logs.',
          description: 'Description for the `A full audit trail, by default` benefit card',
        }),
      },
      {
        title: defineMessage({
          defaultMessage: 'Review where it matters, not everywhere.',
          description: 'Title for the `Review where it matters, not everywhere` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Mark a profile protected and every change goes through submit → review → merge before it\'s live. Leave dev unprotected so nobody\'s waiting on approval to iterate.',
          description: 'Description for the `Review where it matters, not everywhere` benefit card',
        }),
      },
      {
        title: defineMessage({
          defaultMessage: 'Isolated by namespace.',
          description: 'Title for the `Isolated by namespace` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Each namespace gets its own encrypted keyset, backed by Google Tink. A key compromise in one team\'s vault doesn\'t expose another\'s.',
          description: 'Description for the `Isolated by namespace` benefit card',
        }),
      },
    ],
    screenshot: defineMessage({
      defaultMessage: 'The profile configuration editor (/namespace/$namespace/services/$service/profiles/$profile), showing a property row flagged with the red "Invalid property value" alert icon: schema validation catching a bad value inline, before it\'s applied.',
      description: 'Caption describing the screenshot to add for the `Configuration, done right` benefits pillar',
    }),
  },
  {
    title: defineMessage({
      defaultMessage: 'Configuration, made easy',
      description: 'Title for the `Configuration, made easy` benefits pillar',
    }),
    benefits: [
      {
        title: defineMessage({
          defaultMessage: 'No schema to hand-write.',
          description: 'Title for the `No schema to hand-write` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Konfigyr reads the spring-configuration-metadata.json your Gradle or Maven build already produces and pushes it straight to your namespace\'s Artifactory on every build: property names, types, defaults, and deprecations, with nothing for you to author or maintain by hand.',
          description: 'Description for the `No schema to hand-write` benefit card',
        }),
      },
      {
        title: defineMessage({
          defaultMessage: 'Deployed your way.',
          description: 'Title for the `Deployed your way` benefit card',
        }),
        description: defineMessage({
          defaultMessage: 'Same codebase, as managed SaaS or on-premise behind your own firewall: one deployment model, so there\'s no separate tool to run or vendor to manage depending on where it lives.',
          description: 'Description for the `Deployed your way` benefit card',
        }),
      },
    ],
    screenshot: defineMessage({
      defaultMessage: 'The service manifest/catalog view (/namespace/$namespace/services/$service/manifest), listing properties auto-ingested from the build (name, type, default, source artifact version) with no manual entry.',
      description: 'Caption describing the screenshot to add for the `Configuration, made easy` benefits pillar',
    }),
  },
];

export function BenefitsSection() {
  return (
    <Section variant="secondary">
      <SectionHeader className="max-w-[56ch] mx-auto">
        <SectionTitle className="text-3xl text-center">
          <FormattedMessage
            defaultMessage="One platform that actually knows what your configuration means."
            description="Title for the benefits section"
          />
        </SectionTitle>
      </SectionHeader>
      <SectionContent className="gap-12">
        {PILLARS.map((pillar, index) => (
          <div key={index} className="grid gap-6">
            <h3 className="font-heading text-2xl font-semibold text-center">
              <FormattedMessage {...pillar.title} />
            </h3>

            <div className="grid gap-6 sm:grid-cols-2">
              {pillar.benefits.map((benefit, position) => (
                <Card key={position}>
                  <CardContent className="grid gap-2">
                    <h4 className="font-heading text-lg font-semibold">
                      <FormattedMessage {...benefit.title} />
                    </h4>
                    <p className="text-sm text-muted-foreground leading-relaxed">
                      <FormattedMessage {...benefit.description} />
                    </p>
                  </CardContent>
                </Card>
              ))}
            </div>

            <ScreenshotPlaceholder description={pillar.screenshot} />
          </div>
        ))}
      </SectionContent>
    </Section>
  );
}
