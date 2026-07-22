import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | application details', () => {
  afterEach(() => cleanup());

  test('should render namespace application details and form', async () => {
    const { getByLabelText, getByRole } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod');
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
    });
  });

  test('should pass the clientSecret from navigation state through to the application details', async () => {
    const { getByRole, router } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('***********');
    });

    await router.navigate({
      to: '/namespace/$namespace/applications/$id',
      params: { namespace: 'konfigyr', id: 'existing-application-id' },
      state: (prev) => ({ ...prev, clientSecret: 'created-secret' }),
    });

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('created-secret');
    });
  });
});
