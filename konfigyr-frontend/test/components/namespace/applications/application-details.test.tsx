import { afterEach, describe, expect, test, vi } from 'vitest';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import { ApplicationDetails } from '@konfigyr/components/namespace/applications/application-details';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | applications | <ApplicationDetails/>', () => {
  afterEach(() => cleanup());

  test('should render namespace applications details without client secret', () => {
    const { getByText, queryByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    expect(getByText('Client ID:'), 'render client id').toBeInTheDocument();
    expect(getByText('Scopes:'), 'render scopes').toBeInTheDocument();
    expect(queryByText('Client Secret:'), 'not render client secret').not.toBeInTheDocument();

    expect(getByText('Reset application')).toBeInTheDocument();
    expect(getByText('Delete application')).toBeInTheDocument();
  });

  test('should render namespace applications details with client secret', () => {
    const { getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr}
        application={{
          ...applications.konfigyr,
          clientSecret: 'JMAm42MSA0I4_iwzH39Oex0N7qoqSb91z6jEp7LQwQ4',
        }}
      />,
    );

    expect(getByText('Client Secret:'), 'render client secret label').toBeInTheDocument();
    expect(getByText('JMAm42MSA0I4_iwzH39Oex0N7qoqSb91z6jEp7LQwQ4'), 'render client secret value').toBeInTheDocument();
    expect(getByText('Make sure to copy your client secret now as you will not be able to see this again.')).toBeInTheDocument();
  });

  test('should click on the Delete application button', async () => {
    const { queryByText, getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    await waitFor(() => {
      expect(queryByText('Delete application')).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /delete application/i }),
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
    const { queryByText, getByRole, getByText } = renderComponentWithRouter(
      <ApplicationDetails namespace={namespaces.konfigyr} application={applications.konfigyr} />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: /reset application/i })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /reset application/i }),
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
