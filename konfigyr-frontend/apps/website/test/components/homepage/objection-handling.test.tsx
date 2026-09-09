import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ObjectionHandling } from '@konfigyr/components/homepage/objection-handling';

describe('components | homepage | objection-handling', () => {
  test('renders the section headline and all three objections', () => {
    const { getByRole, getByText } = renderWithMessageProvider(<ObjectionHandling />);

    expect(getByRole('heading', { level: 2, name: 'Where Konfigyr fits next to what you already run.' }))
      .toBeInTheDocument();

    expect(getByText(/We already run HashiCorp Vault for secrets/)).toBeInTheDocument();
    expect(getByText(/We're not a Spring Boot shop/)).toBeInTheDocument();
    expect(getByText(/We already use Doppler/)).toBeInTheDocument();
  });
});
