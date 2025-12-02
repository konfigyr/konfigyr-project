import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';


describe('routes | namespace | overview', () => {
  afterEach(() => cleanup());

  test('should render Namespace overview page', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Konfigyr namespace' })).toBeInTheDocument();
    });
  });

  test('should render not found error for unknown Namespace', async () => {
    const { getByText } = renderWithRouter('/namespace/unknown-namespace');

    await waitFor(() => {
      expect(getByText('Namespace with slug \'unknown-namespace\' not found.')).toBeInTheDocument();
    });
  });
});
