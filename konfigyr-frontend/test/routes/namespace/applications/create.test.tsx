import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | applications | create', () => {
  afterEach(() => cleanup());

  test('should render the type selector when no type is selected', async () => {
    const { findByRole } = renderWithRouter('/namespace/konfigyr/applications/create');
    const typeSelector = await findByRole('radiogroup');

    expect(within(typeSelector).getAllByRole('radio')).toHaveLength(3);
    expect(within(typeSelector).getByRole('radio', { name: /service account/i })).toBeInTheDocument();
    expect(within(typeSelector).getByRole('radio', { name: /ai agent/i })).toBeInTheDocument();
    expect(within(typeSelector).getByRole('radio', { name: /workload identity/i })).toBeInTheDocument();
  });

  test('should navigate to the form after selecting a type and clicking continue', async () => {
    const user = userEvent.setup();
    const { findByRole, getByRole, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create');

    await user.click(await findByRole('radio', { name: /service account/i }));

    await user.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    expect(await findByLabelText('Application name')).toBeInTheDocument();
  });

  test('should create a SERVICE_ACCOUNT application', async () => {
    const user = userEvent.setup();
    const { getByRole, findByRole, router, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=SERVICE_ACCOUNT');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await user.click(getByRole('checkbox', { name: 'namespaces:write' }));
    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
    expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');

    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('created-secret');
    expect(getByRole('textbox', { name: 'Scopes' })).toHaveValue('namespaces:read namespaces:write');
  });

  test('should create an AGENT application with a redirect URI', async () => {
    const user = userEvent.setup();
    const { getAllByRole, getByRole, findByRole, router, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=AGENT');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('checkbox', { name: 'namespaces:read' }));

    expect(getByRole('button', { name: 'Add redirect URI' })).toBeInTheDocument();
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(1);

    await user.click(getByRole('button', { name: 'Add redirect URI' }));
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(2);
    expect(getAllByRole('button', { name: 'Remove redirect URI' })).toHaveLength(2);

    await user.click(getAllByRole('button', { name: 'Remove redirect URI' })[0]);
    expect(getAllByRole('textbox', { name: 'Redirect URI' })).toHaveLength(1);

    await user.type(
      getAllByRole('textbox', { name: 'Redirect URI' })[0],
      'https://example.com/callback',
    );

    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
    expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
  });

  test('should create a WORKLOAD application with issuer URI and subject pattern', async () => {
    const user = userEvent.setup();
    const { getByLabelText, getByRole, findByRole, router, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await user.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');
    await user.type(getByLabelText('Subject pattern'), 'repo:owner/name:ref:refs/heads/main');

    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: 'Client ID' })).toHaveValue('created-id');
    expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
  });

  test('should create a WORKLOAD application without a subject pattern', async () => {
    const user = userEvent.setup();
    const { getByLabelText, getByRole, findByRole, router, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('checkbox', { name: 'namespaces:read' }));
    await user.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');

    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: 'Client ID' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/namespace/konfigyr/applications/created-application-id');
  });

  test('should block AGENT submission when no redirect URI is provided', async () => {
    const user = userEvent.setup();
    const { getByRole, findByRole, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=AGENT');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('group', { name: /redirect uris/i })).toBeInTheDocument();
  });

  test('should block WORKLOAD submission when issuer URI is empty', async () => {
    const user = userEvent.setup();
    const { getByRole, findByRole, findByLabelText } = renderWithRouter('/namespace/konfigyr/applications/create?type=WORKLOAD');

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: /issuer uri/i })).toBeInTheDocument();
  });
});
