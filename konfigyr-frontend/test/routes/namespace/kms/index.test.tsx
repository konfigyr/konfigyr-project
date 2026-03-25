import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | KMS', () => {
  afterEach(() => cleanup());

  test('should render namespace key management page with an empty state', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/john-doe/kms');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Create keyset' })).toBeInTheDocument();
      expect(getByText('No keysets found')).toBeInTheDocument();
    });
  });

  test('should render namespace managed keys', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/kms');

    await waitFor(() => {
      expect(getByText('encrypting-keyset')).toBeInTheDocument();
      expect(getByText('signing-keyset')).toBeInTheDocument();
    });
  });
});
