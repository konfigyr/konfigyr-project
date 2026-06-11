import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PropertiesImportDialog } from '@konfigyr/components/vault/properties/properties-import-dialog';
import userEvent from '@testing-library/user-event';
import { profiles, services } from '@konfigyr/test/helpers/mocks';
import React from 'react';
import type { ServiceCatalog } from '@konfigyr/hooks/types';

const PROPERTIES_FILE = 'server.port=8080';
const PARTIAL_PROPERTIES_FILE = `${PROPERTIES_FILE}\nserver.name=konfigyr`;

const YAML_FILE = `
  server:
    port: 8080
`;

const catalog: ServiceCatalog = {
  service: services.konfigyrApi,
  properties: [],
};

const validationCatalog: ServiceCatalog = {
  service: services.konfigyrApi,
  properties: [
    {
      artifact: 'org.springframework.boot:spring-boot:4.0.3',
      name: 'server.port',
      typeName: 'java.lang.Integer',
      schema: { type: 'integer' },
      description: 'Server port',
    },
    {
      artifact: 'org.springframework.boot:spring-boot:4.0.3',
      name: 'server.name',
      typeName: 'java.lang.String',
      schema: { type: 'string' },
      description: 'Server name',
    },
  ],
};

vi.mock('@tanstack/react-start', async () => {
  const { mockTanstackReactStart } = await import('@konfigyr/test/helpers/mocks/tanstack-react-start');
  return mockTanstackReactStart();
});

describe('components | vault | properties | <PropertiesImportDialog/>', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => cleanup());

  test('should render disabled Import button for the immutable profile', () => {
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.deprecated} catalog={catalog}/>,
    );

    expect(result.getByRole('button', { name: 'Import' })).toBeDisabled();
  });

  test('should show empty state by default when dialog is open', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    expect(result.getByText('No configuration properties for import')).toBeInTheDocument();
    expect(result.getByText('Your configuration is empty')).toBeInTheDocument();
    expect(within(result.getByRole('dialog')).getByRole('button', { name: 'Import' })).toBeDisabled();
  });

  test('should import properties and close dialog on successful submit', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File([PROPERTIES_FILE], 'application.properties', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText('The configuration was parsed, and 1 new configuration properties are available for import.')).toBeInTheDocument();
    });

    const dialog = result.getByRole('dialog');
    const importButton = within(dialog).getByRole('button', { name: 'Import' });
    await user.click(importButton);

    await waitFor(() => {
      expect(onImport).toHaveBeenCalledExactlyOnceWith(
        [
          {
            name: 'server.port',
            schema: {
              type: 'string',
            },
            state: 'ADDED',
            typeName: 'string',
            value: {
              decoded: '8080',
              encoded: '8080',
            },
          },

        ]);
    });
    await waitFor(() => expect(result.queryByRole('dialog')).toBeNull());
  });

  test('should import yaml and close dialog on successful submit', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File([YAML_FILE], 'application.yaml', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText('The configuration was parsed, and 1 new configuration properties are available for import.')).toBeInTheDocument();
    });

    const dialog = result.getByRole('dialog');
    const importButton = within(dialog).getByRole('button', { name: 'Import' });
    await user.click(importButton);

    await waitFor(() => {
      expect(onImport).toHaveBeenCalledExactlyOnceWith([
        {
          name: 'server.port',
          schema: {
            type: 'string',
          },
          state: 'ADDED',
          typeName: 'string',
          value: {
            decoded: '8080',
            encoded: '8080',
          },
        },
      ]);
    });
    await waitFor(() => expect(result.queryByRole('dialog')).toBeNull());
  });

  test('should show parsing error for invalid JSON file', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File(['{invalid'], 'application.json', { type: 'application/json' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Could not read your configuration')).toBeInTheDocument();
      expect(result.getByText("Error: Expected property name or '}' in JSON at position 1 (line 1 column 2)")).toBeInTheDocument();
    });
    expect(
      within(
        result.getByRole('dialog')).getByRole('button', { name: 'Import' }),
    ).toBeDisabled();
    expect(onImport).not.toHaveBeenCalled();
  });

  test('should validate config server URL on submit', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());
    await user.click(within(result.getByRole('dialog')).getByRole('tab', { name: 'From config server' }));

    await user.type(result.getByLabelText('User name'), 'test-user');
    await user.type(result.getByLabelText('Password'), 'test-pwd');
    await user.type(result.getByLabelText('Config server URL'), 'not-a-url');

    await user.click(result.getByRole('button', { name: 'Fetch config' }));

    await waitFor(() => {
      expect(result.getByText('Config server URL must be a valid URL')).toBeInTheDocument();
    });
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test('should fetch properties from config server and submit import', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        propertySources: [
          { name: 'highest', source: { 'app.log.level': 'warn' } },
          { name: 'lowest', source: { 'app.environment': 'dev', 'app.log.level': 'info' } },
        ],
      }),
    } as Response);

    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={catalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    await user.click(within(result.getByRole('dialog')).getByRole('tab', { name: 'From config server' }));

    await user.type(result.getByLabelText('User name'), 'test-user');
    await user.type(result.getByLabelText('Password'), 'test-pwd');
    await user.type(result.getByLabelText('Config server URL'), 'http://config.com/api/configs/test-app/dev/master');

    const fetchButton = result.getByRole('button', { name: 'Fetch config' });
    expect(fetchButton).toBeEnabled();
    await user.click(fetchButton);

    await waitFor(() => {
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText('The configuration was parsed, and 2 new configuration properties are available for import.')).toBeInTheDocument();
    });

    const importButton = within(result.getByRole('dialog')).getByRole('button', { name: 'Import' });
    await user.click(importButton);

    await waitFor(() => {
      expect(onImport).toHaveBeenCalledExactlyOnceWith(
        [
          {
            'name': 'app.environment',
            'schema': {
              'type': 'string',
            },
            'state': 'ADDED',
            'typeName': 'string',
            'value': {
              'decoded': 'dev',
              'encoded': 'dev',
            },
          },
          {
            'name': 'app.log.level',
            'schema': {
              'type': 'string',
            },
            'state': 'ADDED',
            'typeName': 'string',
            'value': {
              'decoded': 'warn',
              'encoded': 'warn',
            },
          },
        ],
      );
    });

    expect(fetchSpy).toHaveBeenCalledExactlyOnceWith(
      'http://config.com/api/configs/test-app/dev/master',
      {
        method: 'GET',
        'headers': {
          'Accept': 'application/json',
          'Authorization': 'Basic dGVzdC11c2VyOnRlc3QtcHdk',
        },
      },
    );
  });

  test('should import only valid properties when validation finds mismatches', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport} profile={profiles.development} catalog={validationCatalog}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File([PARTIAL_PROPERTIES_FILE], 'application.properties', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Review imported properties')).toBeInTheDocument();
      expect(result.getByText('Found 1 invalid properties. Continue with valid properties only, or import all and resolve issues afterward.')).toBeInTheDocument();
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText('The configuration was parsed, and 1 new configuration properties are available for import.')).toBeInTheDocument();
      expect(result.getByRole('button', { name: 'Import valid only' })).toBeEnabled();
      expect(result.getByRole('button', { name: 'Import all anyway' })).toBeEnabled();
    });

    await user.click(result.getByRole('button', { name: 'Import valid only' }));

    await waitFor(() => {
      expect(onImport).toHaveBeenCalledExactlyOnceWith([
        {
          description: 'Server name',
          name: 'server.name',
          schema: {
            type: 'string',
          },
          state: 'ADDED',
          typeName: 'string',
          value: {
            decoded: 'konfigyr',
            encoded: 'konfigyr',
          },
        },
      ]);
    });
  });
});
