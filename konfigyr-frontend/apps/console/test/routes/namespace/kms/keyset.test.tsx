import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import { kms } from '@konfigyr/test/helpers/mocks';

describe('routes | namespace | KMS | keyset details', () => {
  afterEach(() => cleanup());

  test('should render keyset details page', async () => {
    const { getByRole } = renderWithRouter(`/namespace/konfigyr/kms/${kms.signingKeyset.id}`);

    await waitFor(() => {
      expect(getByRole('link', { name: kms.signingKeyset.name })).toBeInTheDocument();
    });

    expect(getByRole('button', { name: 'Rotate keyset' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Delete' })).toBeInTheDocument();

    const table = getByRole('table');
    expect(table).toBeInTheDocument();
    expect(table.querySelectorAll('tbody tr')).toHaveLength(kms.signingKeyset.keys.length);
  });
});
