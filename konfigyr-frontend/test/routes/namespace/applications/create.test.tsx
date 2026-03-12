import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';


describe('routes | namespace | applications | create', () => {
  afterEach(() => cleanup());

  test('should create a new application', async () => {
    const { getByLabelText, getByRole, router, container } = renderWithRouter('/namespace/konfigyr/applications/create');

    await waitFor(() => {
      expect(container.querySelector('form[name="create-namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(
      getByLabelText('Application name'),
      'created app',
    );

    await userEvents.click(
      getByRole('checkbox', { name: 'namespaces:read' }),
    );

    expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();

    await userEvents.click(
      getByRole('checkbox', { name: 'namespaces:write' }),
    );

    expect(getByRole('checkbox', { name: 'namespaces:write' })).toBeChecked();

    await userEvents.click(
      getByRole('button', { name: /create application/i }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
    });

    expect(getByRole('textbox', { name: 'Client ID' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');

    expect(getByRole('textbox', { name: 'Client secret' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('created-secret');

    expect(getByRole('textbox', { name: 'Scopes' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Scopes' })).toHaveValue('namespaces:read namespaces:write');
  });

});
