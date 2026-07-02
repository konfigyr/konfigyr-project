import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, queries, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import { applications } from '@konfigyr/test/helpers/mocks';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

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

  test('should render namespace application form with prefilled values', async () => {
    const { getByLabelText, getByRole, getAllByRole } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
      expect(getByLabelText('Expiration date')).toHaveValue('');

      expect(getAllByRole('checkbox')).toHaveLength(10);
      expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();
    });
  });

  test('should render AGENT application details with redirect URIs', async () => {
    const { getByRole, getByText } = renderWithRouter(`/namespace/konfigyr/applications/${applications.agentApplication.id}`);

    await waitFor(() => {
      expect(getByText('AI Agent')).toBeInTheDocument();

      const form = getByRole('form');
      const uriInputs = queries.getAllByRole(form, 'textbox').filter(
        (el) => el.getAttribute('value')?.startsWith('https://'),
      );

      expect(uriInputs).toHaveLength(2);
      expect(uriInputs[0]).toHaveValue('https://example.com/callback');
      expect(uriInputs[1]).toHaveValue('https://other.example.com/callback');
    });
  });

  test('should prefill WORKLOAD form with existing issuer URI and subject pattern', async () => {
    const { getByRole } = renderWithRouter(`/namespace/konfigyr/applications/${applications.workloadApplication.id}`);

    await waitFor(() => {
      const form = getByRole('form');
      expect(queries.getByLabelText(form, 'Issuer URI')).toHaveValue('https://token.actions.githubusercontent.com');
      expect(queries.getByLabelText(form, 'Subject pattern')).toHaveValue('repo:owner/name:ref:refs/heads/main');
    });
  });

  test('should show success toast after updating an application', async () => {
    const user = userEvents.setup();
    const { getByLabelText, getByText } = renderWithRouter('/namespace/konfigyr/applications/existing-application-id');

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
    });

    await user.clear(getByLabelText('Application name'));
    await user.type(getByLabelText('Application name'), 'updated name');

    await user.click(
      getByText(/update application/i),
    );

    await waitFor(() => {
      expect(getByText(/successfully updated/i)).toBeInTheDocument();
    });
  });
});
