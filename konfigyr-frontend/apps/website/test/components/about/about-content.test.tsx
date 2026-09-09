import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { AboutContent } from '@konfigyr/components/about/about-content';

describe('components | about | about-content', () => {
  test('renders the headline and closing CTA', () => {
    const { getByRole } = renderComponentWithRouter(<AboutContent />);

    expect(getByRole('heading', { level: 1, name: 'Why we\'re building Konfigyr' })).toBeInTheDocument();
    expect(getByRole('heading', {
      level: 2,
      name: 'Building on Spring Boot and feeling this pain today?',
    })).toBeInTheDocument();

    expect(getByRole('link', { name: 'Request Early Access' })).toHaveAttribute('href', '/early-access');
  });
});
