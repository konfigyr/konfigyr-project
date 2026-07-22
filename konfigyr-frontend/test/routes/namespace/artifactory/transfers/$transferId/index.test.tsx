import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | transfers | detail', () => {
  afterEach(() => cleanup());

  test('should render the transfer detail page', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/transfers/transfer-incoming-pending/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'com.example.group' })).toBeInTheDocument();
      expect(getByText('Pending')).toBeInTheDocument();
    });
  });

});
