import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';


describe('routes | namespace | applications | create', () => {
  afterEach(() => cleanup());

  test('should create a new application', async () => {
    const { getByLabelText, getByRole, getByText, router, container } = renderWithRouter('/namespace/konfigyr/applications/create');

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

    await userEvents.click(
      getByRole('checkbox', { name: 'namespaces:write' }),
    );

    await userEvents.click(
      getByRole('button', {
        name: /create application/i,
      }),
    );

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application');
    });

    expect(getByText(/client id:/i).closest('p'), 'render client id').toHaveTextContent('Client ID:created-id');
    expect(getByText(/client secret:/i).closest('p'), 'render client id').toHaveTextContent('Client Secret:created-secret');
    expect(getByText(/scopes:/i).closest('p'), 'render scopes').toHaveTextContent('Scopes:namespaces:read namespaces:write');

    expect(getByText('Make sure to copy your client secret now as you will not be able to see this again.'), 'render scopes').toBeInTheDocument();
  });

});
