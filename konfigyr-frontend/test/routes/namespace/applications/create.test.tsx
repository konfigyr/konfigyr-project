import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';


describe('routes | namespace | applications | create', () => {
  afterEach(() => cleanup());

  test('should render the type selector when no type is selected', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/applications/create');

    await waitFor(() => {
      expect(getByRole('radio', { name: /service account/i })).toBeInTheDocument();
      expect(getByRole('radio', { name: /ai agent/i })).toBeInTheDocument();
      expect(getByRole('radio', { name: /workload identity/i })).toBeInTheDocument();
    });
  });

  test('should navigate to the form after selecting a type and clicking continue', async () => {
    const { getByRole, container } = renderWithRouter('/namespace/konfigyr/applications/create');

    await waitFor(() => {
      expect(getByRole('radio', { name: /service account/i })).toBeInTheDocument();
    });

    await userEvents.click(getByRole('radio', { name: /service account/i }));

    await userEvents.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });
  });

  test('should create a SERVICE_ACCOUNT application', async () => {
    const { getByLabelText, getByRole, router, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=SERVICE_ACCOUNT');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await userEvents.click(getByRole('checkbox', { name: 'namespaces:write' }));
    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
    });

    expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('created-secret');
    expect(getByRole('textbox', { name: 'Scopes' })).toHaveValue('namespaces:read namespaces:write');
  });

  test('should create an AGENT application with a redirect URI', async () => {
    const { getAllByRole, getByLabelText, getByRole, router, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=AGENT');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('checkbox', { name: 'namespaces:read' }));

    expect(getByRole('button', { name: 'Add redirect URI' })).toBeInTheDocument();
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(1);

    await userEvents.click(getByRole('button', { name: 'Add redirect URI' }));
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(2);
    expect(getAllByRole('button', { name: 'Remove redirect URI' })).toHaveLength(2);

    await userEvents.click(getAllByRole('button', { name: 'Remove redirect URI' })[0]);
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(1);

    await userEvents.type(
      getAllByRole('textbox', { name: 'Redirect URI' })[0],
      'https://example.com/callback',
    );

    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
    });

    expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
  });

  test('should create a WORKLOAD application with issuer URI and subject pattern', async () => {
    const { getByLabelText, getByRole, router, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await userEvents.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');
    await userEvents.type(getByLabelText('Subject pattern'), 'repo:owner/name:ref:refs/heads/main');

    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
    });

    expect(getByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
  });

  test('should create a WORKLOAD application without a subject pattern', async () => {
    const { getByLabelText, getByRole, router, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await userEvents.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');

    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
    });
  });

  test('should block AGENT submission when no redirect URI is provided', async () => {
    const { getByLabelText, getByRole, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=AGENT');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(getByRole('group', { name: /redirect uris/i })).toBeInTheDocument();
    });
  });

  test('should block WORKLOAD submission when issuer URI is empty', async () => {
    const { getByLabelText, getByRole, container } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await waitFor(() => {
      expect(container.querySelector('form[name="namespace-application-form"]')).toBeInTheDocument();
    });

    await userEvents.type(getByLabelText('Application name'), 'created app');
    await userEvents.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(getByRole('textbox', { name: /issuer uri/i })).toBeInTheDocument();
    });
  });
});
