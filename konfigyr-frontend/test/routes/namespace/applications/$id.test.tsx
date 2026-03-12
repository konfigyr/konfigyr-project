import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | application details', () => {
  afterEach(() => cleanup());

  test('should render namespace application details', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Client ID' })).toBeInTheDocument();
      expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod');

      expect(getByRole('textbox', { name: 'Client secret' })).toBeInTheDocument();
      expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('***********');

      expect(getByRole('textbox', { name: 'Scopes' })).toBeInTheDocument();
      expect(getByRole('textbox', { name: 'Scopes' })).toHaveValue('namespaces:read');

      expect(getByRole('button', { name: 'Reset application' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Delete application' })).toBeInTheDocument();
    });
  });

  test('should render namespace application form with prefiled values', async () => {
    const { getByLabelText, getByRole, getAllByRole } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
      expect(getByLabelText('Expiration date')).toHaveValue('');

      expect(getAllByRole('checkbox')).toHaveLength(10);
      expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();
    });
  });

});
