import { FormattedMessage, defineMessages } from 'react-intl';
import { LegalPlaceholder } from '@konfigyr/components/legal/legal-placeholder';
import {
  Section,
  SectionContent,
  SectionDescription,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

import type { ReactNode } from 'react';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Privacy Policy',
    description: 'The privacy policy page lead',
  },
  effectiveDate: {
    defaultMessage: 'Effective date: <placeholder>[LEGAL: insert date of publication]</placeholder>',
    description: 'Effective date line on the privacy policy page, still carrying a legal review placeholder',
  },
  scope: {
    defaultMessage: 'This policy explains what information Konfigyr collects through this website, why, and what you can do about it. Konfigyr is pre-launch: this site does not yet have a live product, customer accounts, or billing, so this policy covers only what happens when you visit this site or submit the Early Access form.',
    description: 'Privacy policy paragraph describing the scope of the policy',
  },
  operator: {
    defaultMessage: 'Konfigyr is operated by EBF-EDV Beratung Föllmer GmbH, Gustav-Heinemann-Ufer 120-122, 50968 Cologne, Germany (Amtsgericht Köln, HRB 30160).',
    description: 'Privacy policy paragraph naming the legal entity operating Konfigyr',
  },
  controllerTitle: {
    defaultMessage: 'Responsible Party & Data Protection Officer',
    description: 'Title for the responsible party section of the privacy policy',
  },
  controller: {
    defaultMessage: 'The party responsible for data processing on this site (the "controller" under the GDPR) is EBF-EDV Beratung Föllmer GmbH, Gustav-Heinemann-Ufer 120-122, 50968 Cologne, Germany, the same entity named above as Konfigyr\'s current operator.',
    description: 'Privacy policy paragraph naming the GDPR controller',
  },
  dataProtectionOfficer: {
    defaultMessage: '<b>Data Protection Officer:</b> EBF-EDV Beratung Föllmer GmbH has an appointed data protection officer, reachable at the address above. <placeholder>[LEGAL: confirm the correct DPO contact email before publishing]</placeholder>',
    description: 'Privacy policy paragraph about the data protection officer, still carrying a legal review placeholder',
  },
  ukRepresentative: {
    defaultMessage: '<b>UK representative:</b> EBF\'s own privacy policy separately names a UK Art. 27 GDPR representative, activeMind.legal UK Ltd. <placeholder>[LEGAL: confirm whether this appointment already covers Konfigyr specifically]</placeholder>',
    description: 'Privacy policy paragraph about the UK representative, still carrying a legal review placeholder',
  },
  collectTitle: {
    defaultMessage: 'What We Collect',
    description: 'Title for the data collection section of the privacy policy',
  },
  collectForm: {
    defaultMessage: 'When you request early access, we collect what you enter in the form: your name, work email, company name, team size, and anything else you choose to tell us (such as your current tooling or additional context). We don\'t collect anything beyond what\'s submitted through that form.',
    description: 'Privacy policy paragraph describing what the early access form collects',
  },
  collectTechnical: {
    defaultMessage: 'Like most websites, our hosting and analytics infrastructure may automatically log standard technical information (e.g., IP address, browser type, pages visited). <placeholder>[LEGAL: confirm exact analytics/hosting stack in use and disclose accordingly]</placeholder>',
    description: 'Privacy policy paragraph about automatically logged technical information, still carrying a legal review placeholder',
  },
  useTitle: {
    defaultMessage: 'How We Use It',
    description: 'Title for the data usage section of the privacy policy',
  },
  use: {
    defaultMessage: 'We use the information you submit to evaluate early access requests and to follow up with you directly if it looks like a fit for the design-partner program described on this site. We do not sell this information, and we do not use it for advertising.',
    description: 'Privacy policy paragraph describing how submitted information is used',
  },
  useRetention: {
    defaultMessage: '<placeholder>[LEGAL: confirm retention period for early-access submissions that are not converted into a design-partner relationship]</placeholder>',
    description: 'Privacy policy retention paragraph, still an unresolved legal review placeholder',
  },
  sharingTitle: {
    defaultMessage: 'Sharing',
    description: 'Title for the data sharing section of the privacy policy',
  },
  sharing: {
    defaultMessage: 'We don\'t share the information you submit with third parties except as needed to operate this site (for example, a form-processing or email-delivery provider). <placeholder>[LEGAL: name every third-party processor actually in use once the site is built]</placeholder>',
    description: 'Privacy policy paragraph about third party sharing, still carrying a legal review placeholder',
  },
  rightsTitle: {
    defaultMessage: 'Your Rights',
    description: 'Title for the data subject rights section of the privacy policy',
  },
  rights: {
    defaultMessage: 'As EBF-EDV Beratung Föllmer GmbH is a German company based in Cologne, processing of personal data collected through this site is governed by the GDPR. You have the right to access (Art. 15), rectification (Art. 16), erasure (Art. 17), restriction of processing (Art. 18), data portability (Art. 20), and objection (Art. 21) regarding your personal data. To exercise any of these rights, contact the data protection officer named above.',
    description: 'Privacy policy paragraph listing GDPR data subject rights',
  },
  rightsComplaint: {
    defaultMessage: 'You may also lodge a complaint with your local data protection supervisory authority. <placeholder>[LEGAL: confirm whether additional non-EU disclosures, e.g. CCPA/CPRA, are needed]</placeholder>',
    description: 'Privacy policy paragraph about lodging a complaint, still carrying a legal review placeholder',
  },
  changesTitle: {
    defaultMessage: 'Changes to This Policy',
    description: 'Title for the policy changes section of the privacy policy',
  },
  changes: {
    defaultMessage: 'We may update this policy as the product and this site evolve, particularly once Konfigyr has a live product, customer accounts, and billing, all of which will need their own, more detailed privacy terms. We\'ll update the effective date above when that happens.',
    description: 'Privacy policy paragraph about future changes to the policy',
  },
  contactTitle: {
    defaultMessage: 'Contact',
    description: 'Title for the contact section of the privacy policy',
  },
  contact: {
    defaultMessage: 'Questions about this policy: contact the data protection officer named above.',
    description: 'Privacy policy paragraph pointing questions at the data protection officer',
  },
});

const placeholder = (chunks: Array<ReactNode>) => (
  <LegalPlaceholder key="placeholder">{chunks}</LegalPlaceholder>
);

const bold = (chunks: Array<ReactNode>) => (
  <strong key="bold">{chunks}</strong>
);

export function PrivacyPolicy() {
  return (
    <>
      <Section>
        <SectionHeader>
          <SectionTitle render={<h1/>}>
            <FormattedMessage {...messages.lead} />
          </SectionTitle>
          <SectionDescription>
            <FormattedMessage {...messages.effectiveDate} values={{ placeholder }} />
          </SectionDescription>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.scope} tagName="p"/>
          <FormattedMessage {...messages.operator} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.controllerTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.controller} tagName="p"/>
          <FormattedMessage {...messages.dataProtectionOfficer} values={{ b: bold, placeholder }} tagName="p"/>
          <FormattedMessage {...messages.ukRepresentative} values={{ b: bold, placeholder }} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.collectTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.collectForm} tagName="p"/>
          <FormattedMessage {...messages.collectTechnical} values={{ placeholder }} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.useTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.use} tagName="p"/>
          <FormattedMessage {...messages.useRetention} values={{ placeholder }} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.sharingTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.sharing} values={{ placeholder }} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.rightsTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.rights} tagName="p"/>
          <FormattedMessage {...messages.sharing} values={{ placeholder }} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.changesTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.changes} tagName="p"/>
        </SectionContent>
      </Section>

      <Section className="pt-0">
        <SectionHeader>
          <SectionTitle className="text-2xl">
            <FormattedMessage {...messages.contactTitle} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent className="text-muted-foreground leading-relaxed">
          <FormattedMessage {...messages.contact} tagName="p"/>
        </SectionContent>
      </Section>
    </>
  );
}
