import { describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { NotFound } from '@konfigyr/components/routing/not-found';

describe('components | routing | not-found', () => {
  test('renders the 404 message and a link back home', () => {
    const { getByRole } = renderComponentWithRouter(<NotFound />);

    expect(getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Back to homepage' })).toHaveAttribute('href', '/');
  });
});
