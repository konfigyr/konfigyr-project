import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PrivacyPolicy } from '@konfigyr/components/legal/privacy-policy';

describe('components | legal | privacy-policy', () => {
  test('renders the headline and the operating entity', () => {
    const { getByRole, getAllByText } = renderWithMessageProvider(<PrivacyPolicy />);

    expect(getByRole('heading', { level: 1, name: 'Privacy Policy' })).toBeInTheDocument();
    expect(getAllByText(/EBF-EDV Beratung Föllmer GmbH/).length).toBeGreaterThan(0);
  });
});
