import { afterEach, describe, expect, test } from 'vitest';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import { ApplicationDetails } from '@konfigyr/components/namespace/applications/application-details';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | applications | <ApplicationDetails/>', () => {
  afterEach(() => cleanup());

  test('should render service account details without client secret', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr}/>,
    );

    expect(getByRole('textbox', { name: 'Client ID' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('***********');
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveAccessibleDescription(
      'For security reasons, the client secret is never persisted. It is shown only once upon creation or reset. If lost, you must reset the application to generate a new secret.',
    );
    expect(getByRole('textbox', { name: 'Scopes' })).toBeInTheDocument();
    expect(getByText('Service Account')).toBeInTheDocument();

    expect(getByRole('button', { name: 'Reset application' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Delete application' })).toBeInTheDocument();
  });

  test('should render service account details with client secret', () => {
    const { getByRole } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr}
        application={{
          ...applications.konfigyr,
          clientSecret: 'shhh! secret',
        }}
      />,
    );

    expect(getByRole('textbox', { name: 'Client secret' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('shhh! secret');
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveAccessibleDescription(
      'Confidential credential used to authenticate this application with the Identity Provider. Store it securely. You can reset it at any time if compromised.',
    );
  });

  test('should render agent application details with redirect URIs', () => {
    const { getByText, getAllByRole } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.agentApplication}/>,
    );

    expect(getByText('AI Agent')).toBeInTheDocument();

    const uriInputs = getAllByRole('textbox').filter(
      (el) => el.getAttribute('value')?.startsWith('https://'),
    );
    expect(uriInputs).toHaveLength(2);
    expect(uriInputs[0]).toHaveValue('https://example.com/callback');
    expect(uriInputs[1]).toHaveValue('https://other.example.com/callback');
  });

  test('should render workload application details with issuer URI and subject pattern', () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.workloadApplication}/>,
    );

    expect(getByText('Workload Identity')).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Issuer URI' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Issuer URI' })).toHaveValue('https://token.actions.githubusercontent.com');
    expect(getByRole('textbox', { name: 'Subject pattern' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Subject pattern' })).toHaveValue('repo:owner/name:ref:refs/heads/main');
  });

  test('should render workload application details without subject pattern when not set', () => {
    const { getByRole, queryByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails
        namespace={namespaces.konfigyr}
        application={{
          ...applications.workloadApplication,
          settings: { type: 'workload' as const, issuerUri: 'https://token.actions.githubusercontent.com' },
        }}
      />,
    );

    expect(getByText('Workload Identity')).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Issuer URI' })).toBeInTheDocument();
    expect(queryByRole('textbox', { name: 'Subject pattern' })).not.toBeInTheDocument();
  });

  test('should click on the Delete application button', async () => {
    const user = userEvents.setup();
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr}/>,
    );

    expect(getByRole('button', { name: 'Delete application' })).toBeInTheDocument();

    await user.click(
      getByRole('button', { name: 'Delete application' }),
    );

    await waitFor(() => {
      expect(
        getByText((_, e) => e?.textContent === 'Delete konfigyr test application',
        ),
      ).toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    });

    await user.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );
  });

  test('should click on the Reset application button', async () => {
    const user = userEvents.setup();
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr}/>,
    );

    expect(getByRole('button', { name: 'Reset application' })).toBeInTheDocument();

    await user.click(
      getByRole('button', { name: 'Reset application' }),
    );

    await waitFor(() => {
      expect(
        getByText((_, e) => e?.textContent === 'Reset konfigyr test application',
        ),
      ).toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    });

    await user.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );
  });

});
