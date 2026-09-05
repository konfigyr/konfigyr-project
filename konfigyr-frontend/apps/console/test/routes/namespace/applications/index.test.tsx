import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | applications', () => {
  afterEach(() => cleanup());

  test('should render applications list', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/applications');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Create application' })).toBeInTheDocument();
      expect(getByText('konfigyr test')).toBeInTheDocument();
    });
  });
});
