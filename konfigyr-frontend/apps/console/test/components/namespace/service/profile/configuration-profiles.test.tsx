import { afterEach, describe, expect, test, vi } from 'vitest';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import {
  ServiceConfigurationProfiles,
} from '@konfigyr/components/namespace/service/settings/profiles/configuration-profiles';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';

describe('components | namespace | service | profiles | <ServiceConfigurationProfiles/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render <ServiceConfigurationProfiles> component with loading state', () => {
    const result = renderComponentWithRouter(
      <ServiceConfigurationProfiles namespace={namespaces.johnDoe} service={services.konfigyrId} />,
    );

    expect(result.getByText('Configuration profiles')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="configuration-profiles-skeleton"]')).toBeInTheDocument();
  });

  test('should render empty <ServiceConfigurationProfiles> component', async () => {
    const { getByText } = renderComponentWithRouter((
      <ServiceConfigurationProfiles
        namespace={namespaces.johnDoe}
        service={services.konfigyrId}
      />
    ));

    expect(getByText('Configuration profiles')).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('Your service has no configuration profiles yet.')).toBeInTheDocument();
      expect(
        getByText((_, e) => e?.textContent === 'Create a new configuration profile to get started with this service.',
        ),
      ).toBeInTheDocument();
    });

  });

  test('should render <ServiceConfigurationProfiles> component and delete Development profile', async () => {
    const user = userEvents.setup();
    const result = renderComponentWithRouter((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    await waitFor(() => {
      expect(result.getByText('Development')).toBeInTheDocument();
      expect(result.getByText('Staging')).toBeInTheDocument();
    });

    await user.click(
      result.getByRole('button', { name: 'Delete Development profile' }),
    );

    expect(result.getByRole('alertdialog', { name: 'Are you sure you want to delete Development profile?' }))
      .toBeInTheDocument();
    expect(result.getByRole('alertdialog', { name: 'Are you sure you want to delete Development profile?' }))
      .toHaveAccessibleDescription('This action cannot be undone. Deleting the development profile will ' +
        'permanently remove the associated configuration states.If you want to keep the profile but prevent ' +
        'further changes, use the read-only profile policy instead.');

    await user.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    expect(await result.findByText('Staging')).toBeInTheDocument();
    expect(result.queryByText('Development'), 'Development profile was deleted').not.toBeInTheDocument();
  });

  test('should render <ServiceConfigurationProfiles> component and update policy to Read-only for the Development profile', async () => {
    const user = userEvents.setup();
    const result = renderComponentWithRouter((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    await waitFor(() => {
      expect(result.getByText('Development')).toBeInTheDocument();
      expect(result.getByText('Staging')).toBeInTheDocument();
    });

    expect(result.getByRole('button', { name: 'Update policy for Development profile' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Update policy for Development profile' })).toHaveTextContent('Unprotected profile');

    expect(result.getByRole('button', { name: 'Update policy for Staging profile' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Update policy for Staging profile' })).toHaveTextContent('Protected profile');

    await user.click(
      result.getByRole('button', { name: 'Update policy for Development profile' }),
    );

    expect(result.getByRole('menuitemradio', { name: 'Protected' }));
    expect(result.getByRole('menuitemradio', { name: 'Unprotected' }));
    expect(result.getByRole('menuitemradio', { name: 'Read-only' }));

    await user.click(
      result.getByRole('menuitemradio', { name: 'Read-only' }),
    );

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Update policy for Development profile' }))
        .toHaveTextContent('Profile is read-only');
    });
  });

  test('should render <ServiceConfigurationProfiles> component and update Development profile name', async () => {
    const NEW_PROFILE_NAME = 'Development Profile';
    const user = userEvents.setup();

    const result = renderComponentWithRouter((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    await waitFor(() => {
      expect(result.getByText('Development')).toBeInTheDocument();
    });

    expect(result.queryByText(NEW_PROFILE_NAME)).not.toBeInTheDocument();

    await user.click(
      result.getByRole('button', { name: 'Development' }),
    );

    await user.clear(result.getByRole('textbox'));
    await user.type(result.getByRole('textbox'), NEW_PROFILE_NAME);
    await user.keyboard('{Enter}');

    expect(result.getByRole('button', { name: NEW_PROFILE_NAME })).toBeInTheDocument();
  });

  test('should render <ServiceConfigurationProfiles> component and update Development profile description', async () => {
    const NEW_PROFILE_DESCRIPTION = 'Development profile description';
    const user = userEvents.setup();

    const result = renderComponentWithRouter((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    await waitFor(() => {
      expect(result.getByText('Development')).toBeInTheDocument();
    });

    expect(result.queryByText(NEW_PROFILE_DESCRIPTION)).not.toBeInTheDocument();

    await user.click(
      result.getAllByRole('button', { name: 'No description provided' })[0],
    );

    await user.clear(result.getByRole('textbox'));
    await user.type(result.getByRole('textbox'), NEW_PROFILE_DESCRIPTION);
    await user.keyboard('{Enter}');

    expect(result.getByRole('button', { name: NEW_PROFILE_DESCRIPTION })).toBeInTheDocument();
  });

});
