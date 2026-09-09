import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { Faq } from '@konfigyr/components/early-access/faq';

describe('components | early-access | faq', () => {
  test('renders the section headline and all four questions', () => {
    const { getByRole, getByText } = renderWithMessageProvider(<Faq />);

    expect(getByRole('heading', { level: 2, name: 'Frequently asked questions' })).toBeInTheDocument();
    expect(getByText('What happens after I request access?')).toBeInTheDocument();
    expect(getByText('Is there a cost?')).toBeInTheDocument();
    expect(getByText('What do you need from us?')).toBeInTheDocument();
    expect(getByText('We\'re not a Spring Boot shop. Is this still worth submitting?')).toBeInTheDocument();
  });
});
