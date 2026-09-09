import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { Imprint } from '@konfigyr/components/legal/imprint';

describe('components | legal | imprint', () => {
  test('renders the headline and company registration details', () => {
    const { getByRole, getAllByText, getByText } = renderWithMessageProvider(<Imprint />);

    expect(getByRole('heading', { level: 1, name: 'Imprint' })).toBeInTheDocument();
    expect(getAllByText(/EBF-EDV Beratung Föllmer GmbH/).length).toBeGreaterThan(0);
    expect(getByText(/HRB 30160/)).toBeInTheDocument();
    expect(getByText(/DE192361138/)).toBeInTheDocument();
  });
});
