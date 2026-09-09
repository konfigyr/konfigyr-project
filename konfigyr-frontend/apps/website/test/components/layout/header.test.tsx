import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { Header } from '@konfigyr/components/layout/header';

describe('components | layout | header', () => {
  test('renders logo, about link, and early access CTA', () => {
    const { getByRole } = renderComponentWithRouter(<Header />);

    expect(getByRole('link', { name: 'Konfigyr' })).toHaveAttribute('href', '/');
    expect(getByRole('link', { name: 'About' })).toHaveAttribute('href', '/about');
    expect(getByRole('link', { name: 'Request Early Access' })).toHaveAttribute('href', '/early-access');
  });
});
