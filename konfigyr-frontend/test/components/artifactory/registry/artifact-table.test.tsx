import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { artifacts } from '@konfigyr/test/helpers/mocks';
import { ArtifactTable } from '@konfigyr/components/artifactory/registry/artifact-table';

import type { ArtifactDefinition } from '@konfigyr/hooks/artifactory/types';
import type { PageResponse } from '@konfigyr/hooks/hateoas/types';

function page(data: Array<ArtifactDefinition>): PageResponse<ArtifactDefinition> {
  return { data, metadata: { number: 1, size: 20, total: data.length, pages: 1 } };
}

describe('components | registry | <ArtifactTable/>', () => {
  afterEach(() => cleanup());

  test('should render artifacts with their coordinates and visibility indicator', () => {
    const { getByText } = renderComponentWithRouter(
      <ArtifactTable namespace="konfigyr" data={page([artifacts.publicArtifact, artifacts.privateArtifact])} isPending={false}/>,
    );

    expect(getByText('com.example:public-service')).toBeInTheDocument();
    expect(getByText('Public')).toBeInTheDocument();
    expect(getByText('com.example:private-service')).toBeInTheDocument();
    expect(getByText('Private')).toBeInTheDocument();
  });

  test('should render an empty state when no artifacts exist', () => {
    const { getByText } = renderComponentWithRouter(
      <ArtifactTable namespace="konfigyr" data={page([])} isPending={false}/>,
    );

    expect(getByText('No artifacts found')).toBeInTheDocument();
  });
});
