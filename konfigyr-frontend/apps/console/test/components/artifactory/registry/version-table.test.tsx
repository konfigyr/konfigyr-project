import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { artifacts } from '@konfigyr/test/helpers/mocks';
import { VersionTable } from '@konfigyr/components/artifactory/registry/version-table';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

function page(data: Array<VersionedArtifact>): PageResponse<VersionedArtifact> {
  return { data, metadata: { number: 1, size: 20, total: data.length, pages: 1 } };
}

describe('components | registry | <VersionTable/>', () => {
  afterEach(() => cleanup());

  test('should render versions newest first as provided by the caller', () => {
    const { getByText, getAllByRole } = renderComponentWithRouter(
      <VersionTable
        namespace="konfigyr"
        data={page([artifacts.publicArtifactVersionTwo, artifacts.publicArtifactVersionOne])}
        isPending={false}
      />,
    );

    expect(getByText('2.0.0')).toBeInTheDocument();
    expect(getByText('1.0.0')).toBeInTheDocument();
    expect(getAllByRole('row')).toHaveLength(3); // header + 2 versions
  });

  test('should not render pagination controls for a single version', () => {
    const { queryByRole } = renderComponentWithRouter(
      <VersionTable namespace="konfigyr" data={page([artifacts.singleVersion])} isPending={false}/>,
    );

    expect(queryByRole('navigation')).not.toBeInTheDocument();
  });

  test('should render an empty state when no versions exist', () => {
    const { getByText } = renderComponentWithRouter(
      <VersionTable namespace="konfigyr" data={page([])} isPending={false}/>,
    );

    expect(getByText('No versions found')).toBeInTheDocument();
  });
});
