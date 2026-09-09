import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { FinalCta } from '@konfigyr/components/homepage/final-cta';

describe('components | homepage | final-cta', () => {
  test('renders the headline, about link, and early access CTA', () => {
    const { getByRole } = renderComponentWithRouter(<FinalCta />);

    expect(getByRole('heading', {
      level: 2,
      name: 'We\'re taking on a small number of design partners.',
    })).toBeInTheDocument();

    expect(getByRole('link', { name: 'Read more about where we are' })).toHaveAttribute('href', '/about');
    expect(getByRole('link', { name: 'Request Early Access' })).toHaveAttribute('href', '/early-access');
  });
});
