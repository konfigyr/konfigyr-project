import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { SpringCloudConfigurationImporter } from '@konfigyr/components/vault/properties/import/spring-cloud-importer';
import userEvent from '@testing-library/user-event';

import type { PropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

vi.mock('@tanstack/react-start', async () => {
  const { mockTanstackReactStart } = await import('@konfigyr/test/helpers/mocks/tanstack-react-start');
  return mockTanstackReactStart();
});

type Outcome = { properties?: Array<ConfigurationProperty<string>>, error?: Error };

const mockJsonFetch = (body: unknown, init: ResponseInit = {}) => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(() => {
    return Promise.resolve(new Response(JSON.stringify(body), init));
  });
};

describe('components | vault | properties | <SpringCloudConfigurationImporter/>', () => {
  let outcome: Outcome | undefined;

  const importer: PropertiesImporter = async (task) => {
    try {
      outcome = { properties: await task() };
    } catch (error) {
      outcome = { error: error as Error };
    }
  };

  async function fetchConfig (password: string) {
    const user = userEvent.setup();
    const result = renderWithMessageProvider(<SpringCloudConfigurationImporter importer={importer}/>);

    await user.type(result.getByLabelText('User name'), 'user');
    await user.type(result.getByLabelText('Password'), password);
    await user.type(result.getByLabelText('Config server URL'), 'https://config.example.test');
    await user.click(result.getByRole('button', { name: 'Fetch config' }));
  }

  beforeEach(() => {
    outcome = undefined;
    vi.restoreAllMocks();
  });

  afterEach(() => cleanup());

  test('should fetch config from config server and merge propertySources in one object', async () => {
    mockJsonFetch({
      propertySources: [
        { name: 'high', source: { 'feature.flag': true } },
        { name: 'low', source: { 'feature.flag': false, 'service.name': 'api' } },
      ],
    });

    await fetchConfig('password');

    await waitFor(() => expect(outcome).toBeDefined());
    expect(outcome?.error).toBeUndefined();
    expect(outcome?.properties).toMatchObject([
      { name: 'feature.flag', value: { encoded: 'true', decoded: 'true' } },
      { name: 'service.name', value: { encoded: 'api', decoded: 'api' } },
    ]);
  });

  test('should fail when config server response does not contain propertySources', async () => {
    mockJsonFetch({ name: 'broken-payload' });

    await fetchConfig('pass');

    await waitFor(() => expect(outcome).toBeDefined());
    expect(outcome?.properties).toBeUndefined();
    expect(outcome?.error?.message).toContain('Invalid configuration response: missing propertySources');
  });

  test('should fail when Config Server request returns 401', async () => {
    mockJsonFetch('Failed to fetch configuration: 401', { status: 401 });

    await fetchConfig('wrong-password');

    await waitFor(() => expect(outcome).toBeDefined());
    expect(outcome?.properties).toBeUndefined();
    expect(outcome?.error?.message).toBe('Failed to fetch configuration: 401');
  });
});
