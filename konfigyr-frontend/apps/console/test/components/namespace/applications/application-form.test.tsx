import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { NamespaceApplicationForm } from '@konfigyr/components/namespace/applications/application-form';

describe('components | namespace | applications | <NamespaceApplicationForm/>', () => {
  afterEach(() => cleanup());

  test('should create a SERVICE_ACCOUNT application', async () => {
    const handleSubmit = vi.fn();
    const user = userEvent.setup();
    const { findByRole, getByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="SERVICE_ACCOUNT" handleSubmit={handleSubmit}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(await findByRole('checkbox', { name: 'namespaces:read' }));
    await user.click(getByRole('checkbox', { name: 'namespaces:write' }));
    await user.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        name: 'created app',
        type: 'SERVICE_ACCOUNT',
        scopes: 'namespaces:read namespaces:write',
        expiresAt: undefined,
        settings: null,
      });
    });
  });

  test('should create an AGENT application with a redirect URI', async () => {
    const handleSubmit = vi.fn();
    const user = userEvent.setup();
    const { getAllByRole, getByRole, findByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="AGENT" handleSubmit={handleSubmit}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(await findByRole('checkbox', { name: 'namespaces:read' }));

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

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        name: 'created app',
        type: 'AGENT',
        scopes: 'namespaces:read',
        expiresAt: undefined,
        settings: { type: 'agent', redirectUris: ['https://example.com/callback'] },
      });
    });
  });

  test('should create a WORKLOAD application with issuer URI and subject pattern', async () => {
    const handleSubmit = vi.fn();
    const user = userEvent.setup();
    const { getByLabelText, getByRole, findByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="WORKLOAD" handleSubmit={handleSubmit}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(await findByRole('checkbox', { name: 'namespaces:read' }));
    await user.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');
    await user.type(getByLabelText('Subject pattern'), 'repo:owner/name:ref:refs/heads/main');

    await user.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        name: 'created app',
        type: 'WORKLOAD',
        scopes: 'namespaces:read',
        expiresAt: undefined,
        settings: {
          type: 'workload',
          issuerUri: 'https://token.actions.githubusercontent.com',
          subjectPattern: 'repo:owner/name:ref:refs/heads/main',
        },
      });
    });
  });

  test('should create a WORKLOAD application without a subject pattern', async () => {
    const handleSubmit = vi.fn();
    const user = userEvent.setup();
    const { getByLabelText, getByRole, findByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="WORKLOAD" handleSubmit={handleSubmit}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(await findByRole('checkbox', { name: 'namespaces:read' }));
    await user.type(getByLabelText('Issuer URI'), 'https://token.actions.githubusercontent.com');

    await user.click(getByRole('button', { name: /create application/i }));

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        name: 'created app',
        type: 'WORKLOAD',
        scopes: 'namespaces:read',
        expiresAt: undefined,
        settings: {
          type: 'workload',
          issuerUri: 'https://token.actions.githubusercontent.com',
          subjectPattern: undefined,
        },
      });
    });
  });

  test('should block AGENT submission when no redirect URI is provided', async () => {
    const user = userEvent.setup();
    const { getByRole, findByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="AGENT" handleSubmit={vi.fn()}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('group', { name: /redirect uris/i })).toBeInTheDocument();
  });

  test('should block WORKLOAD submission when issuer URI is empty', async () => {
    const user = userEvent.setup();
    const { getByRole, findByRole, findByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} type="WORKLOAD" handleSubmit={vi.fn()}/>,
    );

    await user.type(await findByLabelText('Application name'), 'created app');
    await user.click(getByRole('button', { name: /create application/i }));

    expect(await findByRole('textbox', { name: /issuer uri/i })).toBeInTheDocument();
  });

  test('should render the form with prefilled values for a SERVICE_ACCOUNT application', async () => {
    const { getByLabelText, getByRole, findAllByRole } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} namespaceApplication={applications.konfigyr} type="SERVICE_ACCOUNT" handleSubmit={vi.fn()}/>,
    );

    expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
    expect(getByLabelText('Expiration date')).toHaveValue('');

    expect(await findAllByRole('checkbox')).toHaveLength(10);
    expect(getByRole('checkbox', { name: 'namespaces:read' })).toBeChecked();
  });

  test('should render the form with prefilled redirect URIs for an AGENT application', async () => {
    const { findAllByRole } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} namespaceApplication={applications.agentApplication} type="AGENT" handleSubmit={vi.fn()}/>,
    );

    const uriInputs = await findAllByRole('textbox', { name: 'Redirect URI' });
    expect(uriInputs).toHaveLength(2);
    expect(uriInputs[0]).toHaveValue('https://example.com/callback');
    expect(uriInputs[1]).toHaveValue('https://other.example.com/callback');
  });

  test('should render the form with prefilled issuer URI and subject pattern for a WORKLOAD application', async () => {
    const { getByLabelText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} namespaceApplication={applications.workloadApplication} type="WORKLOAD" handleSubmit={vi.fn()}/>,
    );

    await waitFor(() => {
      expect(getByLabelText('Issuer URI')).toHaveValue('https://token.actions.githubusercontent.com');
      expect(getByLabelText('Subject pattern')).toHaveValue('repo:owner/name:ref:refs/heads/main');
    });
  });

  test('should submit updated form values when editing an existing application', async () => {
    const handleSubmit = vi.fn();
    const user = userEvent.setup();
    const { getByLabelText, getByText } = renderWithQueryClient(
      <NamespaceApplicationForm namespace={namespaces.konfigyr} namespaceApplication={applications.konfigyr} type="SERVICE_ACCOUNT" handleSubmit={handleSubmit}/>,
    );

    await waitFor(() => {
      expect(getByLabelText('Application name')).toHaveValue('konfigyr test');
    });

    await user.clear(getByLabelText('Application name'));
    await user.type(getByLabelText('Application name'), 'updated name');

    await user.click(getByText(/update application/i));

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        name: 'updated name',
        type: 'SERVICE_ACCOUNT',
        scopes: 'namespaces:read',
        expiresAt: undefined,
        settings: null,
      });
    });
  });
});
