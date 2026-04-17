import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | profile | change requests', () => {
  afterEach(() => cleanup());

  test('should render change request list page in loading state', async () => {
    const { container } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-api/requests',
    );

    await waitFor(() => {
      expect(container.querySelector('[data-slot="change-request-list-skeleton"]'))
        .toBeInTheDocument();
    });
  });

  test('should render change request list page with loaded data', async () => {
    const { getByText } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-api/requests',
    );

    await waitFor(() => {
      expect(getByText('Update application name')).toBeInTheDocument();
      expect(getByText('Update datasource URL')).toBeInTheDocument();
    });
  });

  test('should render change request list page in an empty state', async () => {
    const { getByText } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-id/requests',
    );

    await waitFor(() => {
      expect(getByText('There are no change requests yet.')).toBeInTheDocument();
    });
  });

  test('should render change request list page in an error state', async () => {
    const { getByText } = renderWithRouter(
      '/namespace/john-doe/services/unknown/requests',
    );

    await waitFor(() => {
      expect(getByText('Namespace not found')).toBeInTheDocument();
    });
  });

});
