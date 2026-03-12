import { afterEach, describe, expect, test } from 'vitest';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import { ApplicationDetails } from '@konfigyr/components/namespace/applications/application-details';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | applications | <ApplicationDetails/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications details without client secret', () => {
    const { getByRole } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    expect(getByRole('textbox', { name: 'Client ID'})).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toBeInTheDocument();
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveValue('***********');
    expect(getByRole('textbox', { name: 'Client secret' })).toHaveAccessibleDescription(
      'For security reasons, the client secret is never persisted. It is shown only once upon creation or reset. If lost, you must reset the application to generate a new secret.',
    );
    expect(getByRole('textbox', { name: 'Scopes' })).toBeInTheDocument();

    expect(getByRole('button', { name: 'Reset application' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Delete application' })).toBeInTheDocument();
  });

  test('should render namespace applications details with client secret', () => {
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

  test('should click on the Delete application button', async () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    expect(getByRole('button', { name: 'Delete application' })).toBeInTheDocument();

    await userEvents.click(
      getByRole('button', { name: 'Delete application' }),
    );

    await waitFor(() => {
      expect(getByText('Delete "konfigyr test" application'), 'render title of the confirmation window').toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );
  });

  test('should click on the Reset application button', async () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    expect(getByRole('button', { name: 'Reset application' })).toBeInTheDocument();

    await userEvents.click(
      getByRole('button', { name: 'Reset application' }),
    );

    await waitFor(() => {
      expect(getByText('Reset "konfigyr test" application'), 'render title of the confirmation window').toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );
  });

});
