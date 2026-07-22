import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { artifacts, namespaces } from '@konfigyr/test/helpers/mocks';
import { VersionDetail } from '@konfigyr/components/artifactory/registry/version-detail';

describe('components | registry | <VersionDetail/>', () => {
  afterEach(() => cleanup());

  test('should prompt for a search term before showing any properties', () => {
    const { getByText, queryByText } = renderWithQueryClient(
      <VersionDetail namespace={namespaces.konfigyr.slug} version={artifacts.publicArtifactVersionTwo}/>,
    );

    expect(getByText('Search this version\'s properties')).toBeInTheDocument();
    expect(queryByText('example.timeout')).not.toBeInTheDocument();
  });

  test('should show matching properties once a search term is entered', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderWithQueryClient(
      <VersionDetail namespace={namespaces.konfigyr.slug} version={artifacts.publicArtifactVersionTwo}/>,
    );

    await user.type(getByRole('searchbox'), 'timeout');

    await waitFor(() => {
      expect(getByText('example.timeout')).toBeInTheDocument();
    });
  });

  test('should show an empty state when no properties match the search term', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderWithQueryClient(
      <VersionDetail namespace={namespaces.konfigyr.slug} version={artifacts.publicArtifactVersionTwo}/>,
    );

    await user.type(getByRole('searchbox'), 'no-such-property');

    await waitFor(() => {
      expect(getByText('No matching properties found')).toBeInTheDocument();
    });
  });
});
