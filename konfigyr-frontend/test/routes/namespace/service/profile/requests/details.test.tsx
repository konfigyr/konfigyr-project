import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | profile | change requests | details', () => {
  afterEach(() => cleanup());

  test('should render change request details page with loaded data', async () => {
    const { getByRole, getByText } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-api/requests/1',
    );

    await waitFor(() => {
      expect(getByText('Update application name')).toBeInTheDocument();
    });

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Submit review' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Discard change request' })).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('logging.file.max-size')).toBeInTheDocument();
      expect(getByText('logging.level.com.konfigyr.test')).toBeInTheDocument();
      expect(getByText('logging.file.max-history')).toBeInTheDocument();
    });
  });

  test('should render change request details page in an error state', async () => {
    const { getByText } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-api/requests/2',
    );

    await waitFor(() => {
      expect(getByText('Change request not found')).toBeInTheDocument();
    });
  });

});
