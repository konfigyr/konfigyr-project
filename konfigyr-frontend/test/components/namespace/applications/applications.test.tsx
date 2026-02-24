import { afterEach, describe, expect, test } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import { NamespaceApplications } from '@konfigyr/components/namespace/applications/applications';

describe('components | namespace | applications | <NamespaceApplications/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications with loading state', () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Namespace applications')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="namespace-applications-skeleton"]')).toBeInTheDocument();
  });

  test('should render an empty namespace applications component', async () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(result.getByText('Your namespace has no applications yet.')).toBeInTheDocument();
    });
  });

  test('should render namespace applications when loaded', async () => {
    const result = renderComponentWithRouter(
      <NamespaceApplications namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.container.querySelector('[data-slot="namespace-application-article"]')).toBeInTheDocument();
    });
  });
});
