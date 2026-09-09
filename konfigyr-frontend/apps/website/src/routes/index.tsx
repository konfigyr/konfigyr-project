import { createFileRoute } from '@tanstack/react-router';
import { Hero } from '@konfigyr/components/homepage/hero';
import { ProblemSection } from '@konfigyr/components/homepage/problem-section';
import { BenefitsSection } from '@konfigyr/components/homepage/benefits-section';
import { HowItWorks } from '@konfigyr/components/homepage/how-it-works';
import { ObjectionHandling } from '@konfigyr/components/homepage/objection-handling';
import { FinalCta } from '@konfigyr/components/homepage/final-cta';

export const Route = createFileRoute('/')({
  component: HomePage,
});

function HomePage() {
  return (
    <>
      <Hero />
      <ProblemSection />
      <BenefitsSection />
      <HowItWorks />
      <ObjectionHandling />
      <FinalCta />
    </>
  );
}
