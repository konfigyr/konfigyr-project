import { afterEach, describe, expect, test, vi } from 'vitest';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import { NamespaceApplications } from '@konfigyr/components/namespace/applications/applications';

describe('components | namespace | applications | <NamespaceApplications/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications with loading state', () => {
    const result = renderWithQueryClient(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Namespace applications')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="namespace-applications-skeleton"]')).toBeInTheDocument();
  });

  test('should render an empty namespace applications component', async () => {
    const result = renderWithQueryClient(
      <NamespaceApplications namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace has no applications yet.')).toBeInTheDocument();
    });
  });

  test('should render namespace applications when loaded', async () => {
    vi.mock('@tanstack/react-router', () => ({
      Link: ({ children }: any) => <a>{children}</a>,
    }));

    const result = renderWithQueryClient(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.container.querySelector('[data-slot="namespace-application-article"]')).toBeInTheDocument();
    });
  });
});
