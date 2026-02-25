import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';


describe('routes | namespace | application details', () => {
  afterEach(() => cleanup());

  test('should render namespace application details', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/applications/18cVB2BA709FB');

    await waitFor(() => {
      expect(getByText(/client id:/i).closest('p'), 'render client id').toHaveTextContent('Client ID:kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod');
      expect(getByText(/scopes:/i).closest('p'), 'render scopes').toHaveTextContent('Scopes:namespaces:read');

      expect(getByText('Delete application'), 'redner Delete button').toBeInTheDocument();
      expect(getByText('Reset application'), 'redner Reset button').toBeInTheDocument();
    });
  });

  test('should render namespace application form with prefiled values', async () => {
    const { getByLabelText, getByRole } = renderWithRouter('/namespace/konfigyr/applications/18cVB2BA709FB');

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
      expect(getByLabelText('Expiration date')).toHaveValue('');

      expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();
    });
  });

  test.skip('should reset application client secret', async () => {
    const { getByText, getByRole } = renderWithRouter('/namespace/konfigyr/applications/18cVB2BA709FB');

    await waitFor(() => {
      expect(getByText('Reset application'), 'redner Reset button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', {
        name: /reset application/i,
      }),
    );

    await waitFor(() => {
      expect(getByText('Reset "konfigyr test" application'), 'render title of a mddal window').toBeInTheDocument();
      expect(getByText('Are you sure you want to rest "konfigyr test" application? This action cannot be undone.'), 'render body of a mddal window').toBeInTheDocument();

      expect(getByText('Cancel'), 'redner Cancel button').toBeInTheDocument();
      expect(getByText('Yes'), 'redner Yes button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', {
        name: /yes/i,
      }),
    );
  });

});
