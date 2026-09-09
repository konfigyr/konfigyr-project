import { FormattedMessage, defineMessages } from 'react-intl';
import { CallToActionLink } from '@konfigyr/components/cta';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Why we\'re building Konfigyr',
    description: 'The about page lead',
  },
  problem: {
    defaultMessage: 'Teams running Spring Boot microservices at scale lose control over their own configuration. Values drift between dev, staging, and prod. Changes get made by hand, with no validation and no review. And when something breaks, there\'s rarely a reliable record of who changed what, when, or why, especially for the properties that matter most.',
    description: 'The about page paragraph describing the problem Konfigyr started with',
  },
  problemFraming: {
    defaultMessage: 'We didn\'t think that was a secrets-management problem. It\'s a Spring Boot problem, and it needed a Spring Boot-native answer.',
    description: 'The about page paragraph framing the problem as a Spring Boot problem',
  },
  metadata: {
    defaultMessage: 'Every Spring Boot build already generates a full, typed description of its own configuration: <code>spring-configuration-metadata.json</code>. Almost nothing uses it. Generic secret stores and config tools treat every property as an opaque string, regardless of what the framework already knows about it. Konfigyr reads that metadata directly and builds validation, deprecation warnings, and version tracking on top of it, instead of asking teams to hand-author a schema a second time somewhere else.',
    description: 'The about page paragraph explaining why Konfigyr is built on Spring Boot property metadata',
  },
  antiPersona: {
    defaultMessage: 'That\'s also why Konfigyr isn\'t for every team. If none of your services run on Spring Boot, there\'s no metadata for us to read yet, and no value proposition today.',
    description: 'The about page paragraph stating which teams Konfigyr is not for',
  },
  stage: {
    defaultMessage: 'Konfigyr is pre-launch. There\'s no public product, no pricing, and no customers yet. We\'re working directly with a small number of Spring Boot teams as design partners to get the product right before opening it up more broadly. We\'d rather say that plainly than dress it up.',
    description: 'The about page paragraph describing the current pre-launch stage',
  },
  commitments: {
    defaultMessage: 'Konfigyr ships as the same codebase whether it\'s run as managed SaaS or on-premise behind your own firewall. That\'s a deliberate choice, not a roadmap item still pending. And while Spring Boot is the only ecosystem we support today, the underlying registry isn\'t architecturally tied to it. We just don\'t have a committed timeline for anything beyond Spring Boot yet.',
    description: 'The about page paragraph describing what stays true as Konfigyr grows',
  },
  ctaLead: {
    defaultMessage: 'Building on Spring Boot and feeling this pain today?',
    description: 'The about page closing call to action lead',
  },
  ctaSubtitle: {
    defaultMessage: 'We\'re looking for a handful of teams to work with directly before this is a finished product.',
    description: 'The about page closing call to action subtitle',
  },
  cta: {
    defaultMessage: 'Request Early Access',
    description: 'The about page closing call to action button',
  },
});

export function AboutContent() {
  return (
    <>
      <Section>
        <SectionHeader>
          <SectionTitle render={<h1/>}>
            <FormattedMessage {...messages.lead} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent>
          <div className="grid gap-3">
            <p className="text-muted-foreground leading-relaxed">
              <FormattedMessage {...messages.problem} />
            </p>
            <p className="text-muted-foreground leading-relaxed">
              <FormattedMessage {...messages.problemFraming} />
            </p>
          </div>

          <div className="grid gap-3">
            <p className="text-muted-foreground leading-relaxed">
              <FormattedMessage
                {...messages.metadata}
                values={{
                  code: (chunks) => (
                    <code className="text-sm" key="code">
                      {chunks}
                    </code>
                  ),
                }}
              />
            </p>
            <p className="text-muted-foreground leading-relaxed">
              <FormattedMessage {...messages.antiPersona} />
            </p>
          </div>

          <p className="text-muted-foreground leading-relaxed">
            <FormattedMessage {...messages.stage} />
          </p>

          <p className="text-muted-foreground leading-relaxed">
            <FormattedMessage {...messages.commitments} />
          </p>
        </SectionContent>
      </Section>

      <Section variant="secondary" className="text-center">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.ctaLead} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent>
          <p className="text-muted-foreground">
            <FormattedMessage {...messages.ctaSubtitle} />
          </p>
          <div>
            <CallToActionLink size="lg" />
          </div>
        </SectionContent>
      </Section>
    </>
  );
}
