import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | account | settings', () => {
  afterEach(() => cleanup());

  test('should render account settings page', async () => {
    const { getByRole, getByText } = renderWithRouter('/account');

    await waitFor(() => {
      expect(getByText('Display name')).toBeInTheDocument();
      expect(getByText('Email address')).toBeInTheDocument();
      expect(getByRole('button', { name: 'Delete account' })).toBeInTheDocument();
    });
  });
});
