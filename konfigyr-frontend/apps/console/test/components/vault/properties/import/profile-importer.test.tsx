import { afterEach, beforeEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import userEvent from '@testing-library/user-event';
import { NamespaceContext } from '@konfigyr/hooks/namespace/context';
import { ProfileConfigurationImporter } from '@konfigyr/components/vault/properties/import/profile-importer';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';

import type { PropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

type Outcome = { properties?: Array<ConfigurationProperty<string>>, error?: Error };

describe('components | vault | properties | <ProfileConfigurationImporter/>', () => {
  const user = userEvent.setup();
  let outcome: Outcome | undefined;

  const importer: PropertiesImporter = async (task) => {
    try {
      outcome = { properties: await task() };
    } catch (error) {
      outcome = { error: error as Error };
    }
  };

  function renderImporter (current = { service: services.konfigyrId, profile: profiles.development }) {
    return renderWithQueryClient(
      <NamespaceContext.Provider value={namespaces.konfigyr}>
        <ProfileConfigurationImporter
          importer={importer}
          currentService={current.service}
          currentProfile={current.profile}
        />
      </NamespaceContext.Provider>,
    );
  }

  async function choose (combobox: HTMLElement, name: string) {
    await waitFor(() => expect(combobox).toBeEnabled());
    await user.click(combobox);
    await user.click(await within(document.body).findByRole('option', { name }));
  }

  beforeEach(() => {
    outcome = undefined;
  });

  afterEach(() => cleanup());

  test('should ask for the application first and not render the profile select', () => {
    const result = renderImporter();

    expect(result.getAllByRole('combobox')).toHaveLength(1);
    expect(result.getByText('Select an application')).toBeInTheDocument();
    expect(result.getByText('Select the application that owns the profile you want to import first.')).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Load profile' })).toBeDisabled();
  });

  test('should list namespace applications and render the profile select once one is selected', async () => {
    const result = renderImporter();

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);

    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));

    expect(result.getAllByRole('combobox')[0]).toHaveTextContent(services.konfigyrApi.name);
    expect(result.getByText('Select a profile')).toBeInTheDocument();
    expect(result.queryByText('Select the application that owns the profile you want to import first.')).not.toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Load profile' })).toBeDisabled();
  });

  test('should load properties of the selected application profile', async () => {
    const result = renderImporter();

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);
    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));
    await choose(result.getAllByRole('combobox')[1], profiles.staging.name);

    const button = result.getByRole('button', { name: 'Load profile' });
    expect(button).toBeEnabled();
    await user.click(button);

    await waitFor(() => expect(outcome).toBeDefined());

    expect(outcome?.error).toBeUndefined();
    expect(outcome?.properties).toMatchObject([
      {
        name: 'application.name',
        state: 'ADDED',
        value: { encoded: 'konfigyr-frontend', decoded: 'konfigyr-frontend' },
      },
      {
        name: 'application.profile',
        state: 'ADDED',
        value: { encoded: 'staging', decoded: 'staging' },
      },
    ]);
  });

  test('should clear the selected profile when the application changes', async () => {
    const result = renderImporter();

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);
    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));
    await choose(result.getAllByRole('combobox')[1], profiles.development.name);

    expect(result.getByRole('button', { name: 'Load profile' })).toBeEnabled();

    await choose(result.getAllByRole('combobox')[0], services.konfigyrId.name);

    await waitFor(() => expect(result.getByText('Select a profile')).toBeInTheDocument());
    expect(result.getByRole('button', { name: 'Load profile' })).toBeDisabled();
  });

  test('should report the error when the profile properties can not be loaded', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/properties', () => {
        return HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Could not load profile properties.',
        }, { status: 500 });
      }),
    );

    const result = renderImporter();

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);
    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));
    await choose(result.getAllByRole('combobox')[1], profiles.development.name);
    await user.click(result.getByRole('button', { name: 'Load profile' }));

    await waitFor(() => expect(outcome).toBeDefined());

    expect(outcome?.properties).toBeUndefined();
    expect(outcome?.error).toBeInstanceOf(Error);
  });

  test('should not offer the profile that is currently being edited', async () => {
    const result = renderImporter({ service: services.konfigyrApi, profile: profiles.development });

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);
    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));

    const combobox = result.getAllByRole('combobox')[1];
    await waitFor(() => expect(combobox).toBeEnabled());
    await user.click(combobox);

    expect(await within(document.body).findByRole('option', { name: profiles.staging.name })).toBeInTheDocument();
    expect(within(document.body).queryByRole('option', { name: profiles.development.name })).not.toBeInTheDocument();
  });

  test('should offer the same profile of other applications', async () => {
    const result = renderImporter({ service: services.konfigyrApi, profile: profiles.development });

    await choose(result.getByRole('combobox'), services.konfigyrId.name);
    await waitFor(() => expect(result.getAllByRole('combobox')).toHaveLength(2));

    const combobox = result.getAllByRole('combobox')[1];
    await waitFor(() => expect(combobox).toBeEnabled());
    await user.click(combobox);

    expect(await within(document.body).findByRole('option', { name: profiles.development.name })).toBeInTheDocument();
  });

  test('should render the error state when applications can not be loaded', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:namespace/services', () => {
        return HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Could not load applications.',
        }, { status: 500 });
      }),
    );

    const result = renderImporter();

    expect(await result.findByRole('alert')).toBeInTheDocument();
    expect(result.queryByRole('combobox')).not.toBeInTheDocument();
  });

  test('should render the error state when profiles can not be loaded', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles', () => {
        return HttpResponse.json({
          status: 500,
          title: 'Internal server error',
          detail: 'Could not load profiles.',
        }, { status: 500 });
      }),
    );

    const result = renderImporter();

    await choose(result.getByRole('combobox'), services.konfigyrApi.name);

    expect(await result.findByRole('alert')).toBeInTheDocument();
    expect(result.getAllByRole('combobox')).toHaveLength(1);
  });
});
