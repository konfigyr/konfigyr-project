import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | transfers', () => {
  afterEach(() => cleanup());

  test('should render the transfers list with a link to request a transfer', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/transfers');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Request transfer' })).toBeInTheDocument();
      expect(getByText('com.example.group')).toBeInTheDocument();
    });
  });
});
