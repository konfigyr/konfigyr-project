import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | invitations', () => {
  afterEach(() => cleanup());

  test('should render the Namespace invitations page', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/invitations');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Invitations' })).toBeInTheDocument();
    });

    expect(getByRole('table')).toBeInTheDocument();
  });
});
