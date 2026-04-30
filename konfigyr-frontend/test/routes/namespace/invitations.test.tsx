import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | invitations', () => {
  afterEach(() => cleanup());

  test('should render Namespace invitations page', async () => {
    const { getAllByRole, getByRole } = renderWithRouter('/namespace/konfigyr/invitations');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'Invitations' })).toBeInTheDocument();
    });

    expect(getByRole('table')).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Recipient' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Role' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Sender' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Created at' })).toBeInTheDocument();
    expect(getByRole('columnheader', { name: 'Expires at' })).toBeInTheDocument();

    await waitFor(() => {
      expect(getAllByRole('row')).toHaveLength(4);
    });

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();
  });
});
