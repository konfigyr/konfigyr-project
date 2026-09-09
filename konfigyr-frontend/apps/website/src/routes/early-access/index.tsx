import { createServerFn, useServerFn } from '@tanstack/react-start';
import { createFileRoute } from '@tanstack/react-router';
import { Hero } from '@konfigyr/components/early-access/hero';
import { EarlyAccessForm } from '@konfigyr/components/early-access/early-access-form';
import { Faq } from '@konfigyr/components/early-access/faq';
import { Section, SectionContent } from '@konfigyr/components/section';
import { EarlyAccessSubmissionSchema, submitEarlyAccessRequest } from './-handler';

const submitEarlyAccess = createServerFn({ method: 'POST' })
  .validator(EarlyAccessSubmissionSchema)
  .handler(({ data }) => submitEarlyAccessRequest(data));

export const Route = createFileRoute('/early-access/')({
  component: RouteComponent,
  head: () => ({
    meta: [{
      title: 'Request Early Access | Konfigyr',
    }],
  }),
});

function RouteComponent() {
  const action = useServerFn(submitEarlyAccess);

  return (
    <>
      <Hero />
      <Section className="py-4">
        <SectionContent>
          <EarlyAccessForm onSubmit={submission => action({ data: submission })} />
        </SectionContent>
      </Section>
      <Faq />
    </>
  );
}
