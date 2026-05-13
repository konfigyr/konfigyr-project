import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor, within } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PropertiesImportDialog } from '@konfigyr/components/vault/properties/properties-import-dialog';
import userEvent from '@testing-library/user-event';
import React from 'react';

const PROPERTIES_FILE = 'server.port=8080';

const YAML_FILE = `
  server:
    port: 8080
`;

describe('components | vault | properties | <PropertiesImportDialog/>', () => {
  afterEach(() => cleanup());

  test('should import properties and close dialog on successful submit', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockResolvedValue(undefined);
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File([PROPERTIES_FILE], 'application.properties', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText(/there are 1 new configuration properties ready for import/i)).toBeInTheDocument();
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
      <PropertiesImportDialog onImport={onImport}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    await waitFor(() => expect(result.getByRole('dialog')).toBeInTheDocument());

    const input = result.getByLabelText('Select existing configuration file');
    const file = new File([YAML_FILE], 'application.yaml', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => {
      expect(result.getByText('Ready for import')).toBeInTheDocument();
      expect(result.getByText(/there are 1 new configuration properties ready for import/i)).toBeInTheDocument();
    });

    const dialog = result.getByRole('dialog');
    const importButton = within(dialog).getByRole('button', { name: 'Import' });
    await user.click(importButton);

    await waitFor(() => {
      expect(onImport).toHaveBeenCalledExactlyOnceWith( [
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

  test('should keep dialog open and show import error when submit fails', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn().mockRejectedValue(new Error('Import request failed'));
    const result = renderWithMessageProvider(
      <PropertiesImportDialog onImport={onImport}/>,
    );

    await user.click(result.getByRole('button', { name: 'Import' }));
    const input = await result.findByLabelText('Select existing configuration file');
    const file = new File([PROPERTIES_FILE], 'application.properties', { type: 'text/plain' });
    await user.upload(input, file);

    await waitFor(() => expect(result.getByText('Ready for import')).toBeInTheDocument());
    const dialog = result.getByRole('dialog');
    const importButton = within(dialog).getByRole('button', { name: 'Import' });
    await user.click(importButton);

    await waitFor(() => expect(result.getByText('Import request failed')).toBeInTheDocument());
    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(within(result.getByRole('dialog')).getByRole('button', { name: 'Import' })).not.toBeDisabled();
  });

});
