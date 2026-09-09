import { describe, expect, test } from 'vitest';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { HowItWorks } from '@konfigyr/components/homepage/how-it-works';

describe('components | homepage | how-it-works', () => {
  test('renders the section headline and all four steps', () => {
    const { getByRole, getByText } = renderWithMessageProvider(<HowItWorks />);

    expect(getByRole('heading', { level: 2, name: 'From build to production, in four steps.' }))
      .toBeInTheDocument();

    expect(getByText('Publish your metadata.')).toBeInTheDocument();
    expect(getByText('Manage config per profile.')).toBeInTheDocument();
    expect(getByText('Review what needs review.')).toBeInTheDocument();
    expect(getByText('Trace every change.')).toBeInTheDocument();
  });
});
