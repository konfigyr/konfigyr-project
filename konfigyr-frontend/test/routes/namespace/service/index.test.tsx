import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | index', () => {
  afterEach(() => cleanup());

  test('should render the namespace services page', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/services');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Vault' })).toBeInTheDocument();
      expect(getByText('Konfigyr REST API')).toBeInTheDocument();
      expect(getByText('Konfigyr ID')).toBeInTheDocument();
    });
  });
});
