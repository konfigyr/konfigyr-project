import { afterEach, describe, expect, test, vi } from 'vitest';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import {
  ServiceConfigurationProfiles,
} from '@konfigyr/components/namespace/service/settings/profiles/configuration-profiles';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | service | profiles | <ServiceConfigurationProfiles/>', () => {

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render <ServiceConfigurationProfiles> component with loading state', () => {
    const result = renderWithQueryClient(
      <ServiceConfigurationProfiles namespace={namespaces.johnDoe} service={services.konfigyrId} />,
    );

    expect(result.getByText('Configuration profiles')).toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="configuration-profiles-skeleton"]')).toBeInTheDocument();
  });

  test('should render empty <ServiceConfigurationProfiles> component', async () => {

    const { getByText } = renderWithQueryClient((
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

  test('should render open <ServiceConfigurationProfiles> component with two profiles', async () => {
    const { getByText } = renderWithQueryClient((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    expect(getByText('Configuration profiles')).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('Development')).toBeInTheDocument();
      expect(getByText('Staging')).toBeInTheDocument();
    });
  });


  test('should render <ServiceConfigurationProfiles> component and open delete profile alert', async () => {
    const { getByText, getAllByRole } = renderWithQueryClient((
      <ServiceConfigurationProfiles
        namespace={namespaces.konfigyr}
        service={services.konfigyrId}
      />
    ));

    await waitFor(() => {
      expect(getByText('Development')).toBeInTheDocument();
    });

    await userEvents.click(getAllByRole('button', { name: /actions/i })[0]);
    await userEvents.click(getByText(/delete profile/i));

    await waitFor(() => {
      expect(getByText('Delete Development profile')).toBeInTheDocument();
      expect(getByText('Are you sure you want to delete Development configuration profile?')).toBeInTheDocument();
    });

  });

});
