import { useState } from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { artifacts, namespaces } from '@konfigyr/test/helpers/mocks';
import { VersionDetail } from '@konfigyr/components/artifactory/registry/version-detail';

function Harness({ initialTerm }: { initialTerm?: string }) {
  const [term, setTerm] = useState(initialTerm);

  return (
    <VersionDetail
      namespace={namespaces.konfigyr.slug}
      version={artifacts.publicArtifactVersionTwo}
      term={term}
      onTermChange={setTerm}
    />
  );
}

describe('components | registry | <VersionDetail/>', () => {
  afterEach(() => cleanup());

  test('should show every property of the version without requiring a search term', async () => {
    const { getByText } = renderWithQueryClient(<Harness/>);

    await waitFor(() => {
      expect(getByText('example.timeout')).toBeInTheDocument();
      expect(getByText('example.retries')).toBeInTheDocument();
    });
  });

  test('should narrow to matching properties once a search term is entered', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText, queryByText } = renderWithQueryClient(<Harness/>);

    await waitFor(() => expect(getByText('example.retries')).toBeInTheDocument());

    await user.type(getByRole('searchbox'), 'timeout');

    await waitFor(() => {
      expect(getByText('example.timeout')).toBeInTheDocument();
      expect(queryByText('example.retries')).not.toBeInTheDocument();
    });
  });

  test('should show an empty state when no properties match the search term', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderWithQueryClient(<Harness/>);

    await user.type(getByRole('searchbox'), 'no-such-property');

    await waitFor(() => {
      expect(getByText('No matching properties found')).toBeInTheDocument();
    });
  });

  test('should not show which artifact or namespace owns the property', async () => {
    const { getByText, queryByText } = renderWithQueryClient(<Harness/>);

    await waitFor(() => expect(getByText('example.timeout')).toBeInTheDocument());

    expect(queryByText(/Owned by/)).not.toBeInTheDocument();
  });

  test('should call onTermChange when the search field changes', async () => {
    const user = userEvents.setup();
    const onTermChange = vi.fn();

    const { getByRole } = renderWithQueryClient(
      <VersionDetail
        namespace={namespaces.konfigyr.slug}
        version={artifacts.publicArtifactVersionTwo}
        onTermChange={onTermChange}
      />,
    );

    await user.type(getByRole('searchbox'), 'timeout');

    await waitFor(() => expect(onTermChange).toHaveBeenCalledWith('timeout'));
  });
});
