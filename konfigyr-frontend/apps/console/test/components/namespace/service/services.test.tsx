import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { NamespaceServices } from '@konfigyr/components/namespace/service/services';

describe('components | namespace | service | <NamespaceServices/>', () => {
  afterEach(() => cleanup());

  test('should render namespace services with loading state', () => {
    const result = renderComponentWithRouter(
      <NamespaceServices namespace={namespaces.konfigyr} />,
    );

    expect(result.getByText('Services')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="namespace-service-skeleton"]')).toBeInTheDocument();
  });

  test('should render an empty namespace services component', async () => {
    const result = renderComponentWithRouter(
      <NamespaceServices namespace={namespaces.johnDoe} />,
    );

    await waitFor(() => {
      expect(result.getByText('Your vault has no services yet.')).toBeInTheDocument();
      expect(result.getByText('Create a new service to get started.')).toBeInTheDocument();
    });
  });

  test('should render an error state when the namespace can not be found', async () => {
    const result = renderComponentWithRouter(
      <NamespaceServices namespace={namespaces.unknown} />,
    );

    await waitFor(() => {
      expect(result.getByText('Namespace not found')).toBeInTheDocument();
    });
  });

  test('should render namespace services when loaded', async () => {
    const result = renderComponentWithRouter(
      <NamespaceServices namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.getByText('Konfigyr REST API')).toBeInTheDocument();
      expect(result.getByText('Konfigyr REST API service')).toBeInTheDocument();
      expect(result.getByText('Konfigyr ID')).toBeInTheDocument();
      expect(result.getByText('Konfigyr identity server')).toBeInTheDocument();
    });
  });

  test('should link each service to its detail page', async () => {
    const result = renderComponentWithRouter(
      <NamespaceServices namespace={namespaces.konfigyr} />,
    );

    await waitFor(() => {
      expect(result.getByRole('link', { name: /Konfigyr REST API/ })).toHaveAttribute(
        'href', '/namespace/konfigyr/services/konfigyr-api',
      );
      expect(result.getByRole('link', { name: /Konfigyr ID/ })).toHaveAttribute(
        'href', '/namespace/konfigyr/services/konfigyr-id',
      );
    });
  });
});
