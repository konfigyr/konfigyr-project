import { afterEach, describe, expect, test } from 'vitest';
import { Members } from '@konfigyr/components/namespace/members/members';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';

describe('components | namespace | members | <Members/>', () => {
  afterEach(() => cleanup());

  test('should render namespace members with loading state', () => {
    const result = renderWithQueryClient(
      <Members namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Namespace members')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="member-skeleton"]')).toBeInTheDocument();
  });

  test('should render an empty namespace members component', async () => {
    const result = renderWithQueryClient(
      <Members namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace has no members yet.')).toBeInTheDocument();
    });
  });

  test('should render namespace members when loaded', async () => {
    const result = renderWithQueryClient(
      <Members namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.container.querySelector('[data-slot="member-article"]')).toBeInTheDocument();
    });
  });
});
