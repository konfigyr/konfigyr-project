import { FormattedMessage, defineMessages } from 'react-intl';
import { LegalPlaceholder } from '@konfigyr/components/legal/legal-placeholder';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

import type { ReactNode } from 'react';
import type { MessageDescriptor } from 'react-intl';

const messages = defineMessages({
  lead: {
    defaultMessage: 'Imprint',
    description: 'The imprint page lead',
  },
  companyTitle: {
    defaultMessage: 'Company Information',
    description: 'Title for the company information section of the imprint',
  },
  usaRepresentation: {
    defaultMessage: 'Representation in the USA:',
    description: 'Label above the United States representation address in the imprint',
  },
  website: {
    defaultMessage: 'Website: {domain}',
    description: 'Website line in the imprint company information block',
  },
  email: {
    defaultMessage: 'Email: {address}',
    description: 'Email line in the imprint company information block',
  },
  registrationTitle: {
    defaultMessage: 'Management & Registration',
    description: 'Title for the management and registration section of the imprint',
  },
  directors: {
    defaultMessage: 'Authorized representative managing directors: {names}',
    description: 'Managing directors line in the imprint registration block',
  },
  court: {
    defaultMessage: 'Registration court: {court}',
    description: 'Registration court line in the imprint registration block',
  },
  registerNumber: {
    defaultMessage: 'Register number: {number}',
    description: 'Commercial register number line in the imprint registration block',
  },
  vat: {
    defaultMessage: 'VAT identification number in accordance with §27a VAT Act: {number}',
    description: 'VAT identification number line in the imprint registration block',
  },
  insurance: {
    defaultMessage: 'Business liability insurance: {insurer}',
    description: 'Business liability insurance line in the imprint registration block',
  },
  contentTitle: {
    defaultMessage: 'Content Responsibility',
    description: 'Title for the content responsibility section of the imprint',
  },
  contentResponsibility: {
    defaultMessage: 'Person responsible for content in accordance with Section 18(2) MStV: {names}',
    description: 'Content responsibility line naming the people responsible under German media law',
  },
  contentResponsibilityReview: {
    defaultMessage: '<placeholder>[LEGAL: confirm whether this disclosure is required given Konfigyr\'s current page set (no blog/editorial content)]</placeholder>',
    description: 'Content responsibility paragraph, still an unresolved legal review placeholder',
  },
  disputeTitle: {
    defaultMessage: 'Alternative Dispute Resolution',
    description: 'Title for the alternative dispute resolution section of the imprint',
  },
  dispute: {
    defaultMessage: 'EBF-EDV Beratung Föllmer GmbH does not commit to nor is it obliged to participate in the alternative dispute resolution for consumer disputes in front of a consumer dispute resolution entity.',
    description: 'Alternative dispute resolution statement in the imprint',
  },
  copyrightTitle: {
    defaultMessage: 'Copyright',
    description: 'Title for the copyright section of the imprint',
  },
  copyright: {
    defaultMessage: 'The content and works published on this website by the website operator are subject to German copyright law. The reproduction, processing, distribution and any kind of utilization beyond the scope of copyright law require the written consent of the relevant author/creator. Downloads and copies obtained from this site are only allowed for private, non-commercial use. Third-party copyright has been observed in all cases where the operator of this website did not create the content. Third-party content in particular is identified as such. We would nevertheless ask you to notify us accordingly if you encounter any infringement of copyright. We will remove any such content immediately once a corresponding breach of the law is made known to us.',
    description: 'Copyright notice in the imprint',
  },
});

const placeholder = (chunks: Array<ReactNode>) => (
  <LegalPlaceholder key="placeholder">{chunks}</LegalPlaceholder>
);

function CompanyInformation({ name, address, city, phone, fax }: {
  name: string;
  address: string;
  city: string;
  phone: string;
  fax?: string
}) {
  return (
    <div className="text-muted-foreground leading-relaxed">
      <p>{name}</p>
      <p>{address}</p>
      <p>{city}</p>
      <p>Tel.: {phone}</p>
      {fax && <p>Fax: {fax}</p>}
    </div>
  );
}

function ImprintSection({ title, children }: { title: MessageDescriptor, children: ReactNode }) {
  return (
    <Section className="pt-0">
      <SectionHeader>
        <SectionTitle className="text-2xl">
          <FormattedMessage {...title} />
        </SectionTitle>
      </SectionHeader>
      <SectionContent className="text-muted-foreground leading-relaxed">
        {children}
      </SectionContent>
    </Section>
  );
}

export function Imprint() {
  return (
    <>
      <Section>
        <SectionHeader>
          <SectionTitle render={<h1 />}>
            <FormattedMessage {...messages.lead} />
          </SectionTitle>
        </SectionHeader>
        <SectionContent>
          <h2 className="text-xl font-heading font-semibold">
            <FormattedMessage {...messages.companyTitle} />
          </h2>
          <CompanyInformation
            name="EBF-EDV Beratung Föllmer GmbH"
            address="Gustav-Heinemann-Ufer 120-122"
            city="50968 Cologne"
            phone="+49 221 47455-0"
            fax="+49 221 47455-111"
          />

          <p className="font-medium text-foreground">
            <FormattedMessage {...messages.usaRepresentation} />
          </p>
          <CompanyInformation
            name="EBF Inc."
            address="1221 Hermosa Ave STE 101"
            city="Hermosa Beach, CA 90254, USA"
            phone="+1 310 9802781"
          />

          <div className="text-muted-foreground leading-relaxed">
            <FormattedMessage {...messages.website} values={{ domain: 'konfigyr.com' }} tagName="p" />
            <FormattedMessage {...messages.email} values={{ address: 'contact@konfigyr.com' }} tagName="p"/>
          </div>
        </SectionContent>
      </Section>

      <ImprintSection title={messages.registrationTitle}>
        <div className="gap gap-1">
          <FormattedMessage {...messages.directors} values={{ names: 'Markus Adolph, Marco Föllmer' }} tagName="p"/>
          <FormattedMessage {...messages.court} values={{ court: 'Amtsgericht Köln' }} tagName="p"/>
          <FormattedMessage {...messages.registerNumber} values={{ number: 'HRB 30160' }} tagName="p"/>
          <FormattedMessage {...messages.vat} values={{ number: 'DE192361138' }} tagName="p"/>
          <FormattedMessage {...messages.insurance} values={{ insurer: 'Hiscox' }} tagName="p"/>
        </div>
      </ImprintSection>

      <ImprintSection title={messages.contentTitle}>
        <FormattedMessage
          {...messages.contentResponsibility}
          values={{ names: 'Markus Adolph, Marco Föllmer' }}
          tagName="p"
        />
        <FormattedMessage {...messages.contentResponsibilityReview} values={{ placeholder }} tagName="p"/>
      </ImprintSection>

      <ImprintSection title={messages.disputeTitle}>
        <FormattedMessage {...messages.dispute} tagName="p"/>
      </ImprintSection>

      <ImprintSection title={messages.copyrightTitle}>
        <FormattedMessage {...messages.copyright} tagName="p"/>
      </ImprintSection>
    </>
  );
}
