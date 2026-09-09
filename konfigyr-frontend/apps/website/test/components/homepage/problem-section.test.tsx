import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ProblemSection } from '@konfigyr/components/homepage/problem-section';

describe('components | homepage | problem-section', () => {
  test('renders the section headline', () => {
    const { getByRole } = renderWithMessageProvider(<ProblemSection />);

    expect(getByRole('heading', { level: 2, name: 'You already know what this looks like.' }))
      .toBeInTheDocument();
  });
});
