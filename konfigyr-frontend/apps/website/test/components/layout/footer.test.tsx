import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { Footer } from '@konfigyr/components/layout/footer';

describe('components | layout | footer', () => {
  test('renders legal and navigation links', () => {
    const { getByRole, getByText } = renderComponentWithRouter(<Footer />);

    expect(getByRole('link', { name: 'About' })).toHaveAttribute('href', '/about');
    expect(getByRole('link', { name: 'Privacy' })).toHaveAttribute('href', '/privacy');
    expect(getByRole('link', { name: 'Imprint' })).toHaveAttribute('href', '/imprint');
    expect(getByRole('link', { name: 'Request Early Access' })).toHaveAttribute('href', '/early-access');
    expect(getByText(/EBF-EDV Beratung Föllmer GmbH/)).toBeInTheDocument();
  });
});
