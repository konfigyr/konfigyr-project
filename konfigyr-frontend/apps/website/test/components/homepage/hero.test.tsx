import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { Hero } from '@konfigyr/components/homepage/hero';

describe('components | homepage | hero', () => {
  test('renders the headline and early access CTA', () => {
    const { getByRole, getByText } = renderComponentWithRouter(<Hero />);

    expect(getByRole('heading', {
      level: 1,
      name: 'Configuration management for Spring Boot.',
    })).toBeInTheDocument();

    expect(getByText(/Not a generic secrets store with a UI on top/)).toBeInTheDocument();

    const cta = getByRole('link', { name: 'Request Early Access' });
    expect(cta).toHaveAttribute('href', '/early-access');
  });
});
