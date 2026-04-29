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

    await userEvents.click(result.getAllByTestId('delete-profile-button')[0]);

    await waitFor(() => {
      expect(result.getByText('Delete Development profile')).toBeInTheDocument();
      expect(result.getByText('Are you sure you want to delete Development configuration profile?')).toBeInTheDocument();
    });

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    expect(await result.findByText('Staging')).toBeInTheDocument();
    expect(result.queryByText('Development')).not.toBeInTheDocument();
  });

});
