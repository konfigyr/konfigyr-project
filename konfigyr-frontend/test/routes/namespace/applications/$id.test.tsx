import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | application details', () => {
  afterEach(() => cleanup());

  test('should render namespace application details', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByText(/client id:/i).closest('p'), 'render client id').toHaveTextContent('Client ID:kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod');
      expect(getByText(/scopes:/i).closest('p'), 'render scopes').toHaveTextContent('Scopes:namespaces:read');

      expect(getByText('Delete application'), 'redner Delete button').toBeInTheDocument();
      expect(getByText('Reset application'), 'redner Reset button').toBeInTheDocument();
    });
  });

  test('should render namespace application form with prefiled values', async () => {
    const { getByLabelText, getByRole } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
      expect(getByLabelText('Expiration date')).toHaveValue('');

      expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();
    });
  });

});
