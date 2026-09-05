import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | artifactory | search', () => {
  afterEach(() => cleanup());

  test('should show every visible property by default, without requiring a search term', async () => {
    const { findByText, queryByText } = renderWithRouter('/namespace/konfigyr/artifactory/search');

    expect(await findByText('example.timeout')).toBeInTheDocument();
    expect(await findByText('internal.secret.rotation-interval')).toBeInTheDocument();
    expect(await findByText('notes.storage.path')).toBeInTheDocument();
    expect(queryByText('secret.internal.token')).not.toBeInTheDocument();
  });

  test('should never show a private property owned by a different namespace', async () => {
    const { findByText, queryByText } = renderWithRouter('/namespace/john-doe/artifactory/search');

    expect(await findByText('notes.storage.path')).toBeInTheDocument();
    expect(await findByText('secret.internal.token')).toBeInTheDocument();
    expect(await findByText('example.timeout')).toBeInTheDocument();
    expect(queryByText('internal.secret.rotation-interval')).not.toBeInTheDocument();
  });

  test('should narrow results down to a matching search term', async () => {
    const user = userEvents.setup();

    const { findByRole, findByText, queryByText } = renderWithRouter('/namespace/konfigyr/artifactory/search');

    await findByText('notes.storage.path');

    await user.type(await findByRole('searchbox'), 'internal');

    await waitFor(() => {
      expect(queryByText('notes.storage.path')).not.toBeInTheDocument();
    });

    expect(await findByText('internal.secret.rotation-interval')).toBeInTheDocument();
    expect(queryByText('example.timeout')).not.toBeInTheDocument();
  });

  test('should sync the search term to the URL', async () => {
    const user = userEvents.setup();

    const { findByRole, router } = renderWithRouter('/namespace/konfigyr/artifactory/search');

    await user.type(await findByRole('searchbox'), 'internal');

    await waitFor(() => {
      expect(router.state.location.search).toMatchObject({ term: 'internal', page: 1 });
    });
  });

  test('should show a "no results" state when nothing matches the search term', async () => {
    const user = userEvents.setup();

    const { findByRole, findByText } = renderWithRouter('/namespace/konfigyr/artifactory/search');

    await user.type(await findByRole('searchbox'), 'no-such-property');

    expect(await findByText('No matching properties found')).toBeInTheDocument();
  });

  test('should show a result\'s owning artifact coordinates and namespace as plain text, not a link', async () => {
    const { findByText, queryByRole } = renderWithRouter('/namespace/konfigyr/artifactory/search');

    expect(await findByText('io.johndoe:notes-service')).toBeInTheDocument();
    expect(queryByRole('link', { name: 'io.johndoe:notes-service' })).not.toBeInTheDocument();
  });
});
