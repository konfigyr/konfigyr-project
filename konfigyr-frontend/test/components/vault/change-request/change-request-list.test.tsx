import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { ChangeRequestList } from '@konfigyr/components/vault/change-request/change-request-list';

describe('components | vault | <ChangeRequestList/>', () => {
  afterEach(() => cleanup());

  test('should render a loading skeleton', () => {
    const { container } = renderComponentWithRouter(
      <ChangeRequestList namespace={namespaces.konfigyr} service={services.konfigyrApi} query={{}} onQueryChange={vi.fn()}/>,
    );

    expect(container.querySelector('[data-slot="change-request-list-skeleton"]')).toBeInTheDocument();
  });

  test('should render the loaded change requests', async () => {
    const { getByText } = renderComponentWithRouter(
      <ChangeRequestList namespace={namespaces.konfigyr} service={services.konfigyrApi} query={{}} onQueryChange={vi.fn()}/>,
    );

    await waitFor(() => {
      expect(getByText('Update application name')).toBeInTheDocument();
      expect(getByText('Update datasource URL')).toBeInTheDocument();
    });
  });

  test('should render an empty state when there are no change requests', async () => {
    const { getByText } = renderComponentWithRouter(
      <ChangeRequestList namespace={namespaces.konfigyr} service={services.konfigyrId} query={{}} onQueryChange={vi.fn()}/>,
    );

    await waitFor(() => {
      expect(getByText('There are no change requests yet.')).toBeInTheDocument();
    });
  });

  test('should render an error state when the change requests fail to load', async () => {
    const { getByText } = renderComponentWithRouter(
      <ChangeRequestList namespace={namespaces.johnDoe} service={services.konfigyrApi} query={{}} onQueryChange={vi.fn()}/>,
    );

    await waitFor(() => {
      expect(getByText('Namespace not found')).toBeInTheDocument();
    });
  });
});
