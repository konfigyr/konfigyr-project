import { FormattedMessage, defineMessage } from 'react-intl';
import {
  Section,
  SectionContent,
  SectionHeader,
  SectionTitle,
} from '@konfigyr/components/section';

import type { MessageDescriptor } from 'react-intl';

interface FaqItem {
  question: MessageDescriptor;
  answer: MessageDescriptor;
}

const FAQ_ITEMS: Array<FaqItem> = [
  {
    question: defineMessage({
      defaultMessage: 'What happens after I request access?',
      description: 'Question for the `what happens after I request access` FAQ entry',
    }),
    answer: defineMessage({
      defaultMessage: 'We\'ll read your request and reach out directly if it looks like a fit. There\'s no automatic approval or waitlist queue to track.',
      description: 'Answer for the `what happens after I request access` FAQ entry',
    }),
  },
  {
    question: defineMessage({
      defaultMessage: 'Is there a cost?',
      description: 'Question for the `is there a cost` FAQ entry',
    }),
    answer: defineMessage({
      defaultMessage: 'We haven\'t finalized pricing yet. Design partners get access ahead of that, and a direct hand in shaping what we build before it\'s priced.',
      description: 'Answer for the `is there a cost` FAQ entry',
    }),
  },
  {
    question: defineMessage({
      defaultMessage: 'What do you need from us?',
      description: 'Question for the `what do you need from us` FAQ entry',
    }),
    answer: defineMessage({
      defaultMessage: 'Mainly a willingness to run pre-release software and tell us honestly what does and doesn\'t work. In exchange, you get direct input into the product and access before anyone else.',
      description: 'Answer for the `what do you need from us` FAQ entry',
    }),
  },
  {
    question: defineMessage({
      defaultMessage: 'We\'re not a Spring Boot shop. Is this still worth submitting?',
      description: 'Question for the `not a Spring Boot shop` FAQ entry',
    }),
    answer: defineMessage({
      defaultMessage: 'Spring Boot is the only ecosystem Konfigyr supports today, so if none of your services run on it, there\'s nothing to try yet. If you\'re mixed, some Spring Boot and some not, it\'s still worth a conversation.',
      description: 'Answer for the `not a Spring Boot shop` FAQ entry',
    }),
  },
];

export function Faq() {
  return (
    <>
      <Section>
        <SectionHeader>
          <SectionTitle className="text-3xl">
            <FormattedMessage
              defaultMessage="Frequently asked questions"
              description="Title for the early access FAQ section"
            />
          </SectionTitle>
        </SectionHeader>
        <SectionContent>
          <dl className="grid gap-6">
            {FAQ_ITEMS.map((item, index) => (
              <div key={index}>
                <dt className="font-medium">
                  <FormattedMessage {...item.question} />
                </dt>
                <dd className="mt-1 text-sm text-muted-foreground leading-relaxed">
                  <FormattedMessage {...item.answer} />
                </dd>
              </div>
            ))}
          </dl>
        </SectionContent>
      </Section>
    </>
  );
}
