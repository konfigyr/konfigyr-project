import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw/http';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import { server } from '@konfigyr/test/helpers/server';
import { clearChangesetStorage } from '@konfigyr/hooks/vault/changeset-storage';

describe('routes | namespace | service | profile', () => {
  afterEach(() => {
    cleanup();
    clearChangesetStorage();
  });

  test('should render profile configuration overview with the changeset editor', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/profiles/development');

    await waitFor(() => {
      expect(getByText('Configuration overview')).toBeInTheDocument();
      expect(getByRole('table')).toBeInTheDocument();
    });

    expect(getByText('application.name')).toBeInTheDocument();
    expect(getByText('application.profile')).toBeInTheDocument();
  });

  test('should render an error state when the profile properties cannot be retrieved', async () => {
    server.use(
      http.get('http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/properties', () => (
        HttpResponse.json({
          status: 500,
          title: 'Vault unavailable',
          detail: 'Could not unseal the profile vault.',
        }, { status: 500 })
      )),
    );

    const { getByText, queryByRole } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/profiles/development');

    await waitFor(() => {
      expect(getByText('Vault unavailable')).toBeInTheDocument();
    });

    expect(getByText('Could not unseal the profile vault.')).toBeInTheDocument();
    expect(queryByRole('table')).not.toBeInTheDocument();
  });
});
