import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { BenefitsSection } from '@konfigyr/components/homepage/benefits-section';

describe('components | homepage | benefits-section', () => {
  afterEach(() => cleanup());

  test('renders the section headline, both pillars, and every benefit', () => {
    const { getByRole } = renderWithMessageProvider(<BenefitsSection />);

    expect(getByRole('heading', {
      level: 2,
      name: 'One platform that actually knows what your configuration means.',
    })).toBeInTheDocument();

    expect(getByRole('heading', { level: 3, name: 'Configuration, done right' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 3, name: 'Configuration, made easy' })).toBeInTheDocument();

    expect(getByRole('heading', { level: 4, name: 'Type-safe, before it ships.' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 4, name: 'A full audit trail, by default.' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 4, name: 'Review where it matters, not everywhere.' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 4, name: 'Isolated by namespace.' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 4, name: 'No schema to hand-write.' })).toBeInTheDocument();
    expect(getByRole('heading', { level: 4, name: 'Deployed your way.' })).toBeInTheDocument();
  });

  test('renders a screenshot placeholder at the end of each pillar', () => {
    const { getAllByText, getByText } = renderWithMessageProvider(<BenefitsSection />);

    expect(getAllByText('Screenshot placeholder: not yet added')).toHaveLength(2);

    expect(getByText(/profile configuration editor/)).toBeInTheDocument();
    expect(getByText(/Invalid property value/)).toBeInTheDocument();

    expect(getByText(/service manifest\/catalog view/)).toBeInTheDocument();
    expect(getByText(/no manual entry/)).toBeInTheDocument();
  });
});
