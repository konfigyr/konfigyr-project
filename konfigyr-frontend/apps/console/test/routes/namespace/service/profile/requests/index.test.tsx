import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | profile | change requests', () => {
  afterEach(() => cleanup());

  test('should render the change request list page', async () => {
    const { getByText } = renderWithRouter(
      '/namespace/konfigyr/services/konfigyr-api/requests',
    );

    await waitFor(() => {
      expect(getByText('Update application name')).toBeInTheDocument();
    });
  });
});
