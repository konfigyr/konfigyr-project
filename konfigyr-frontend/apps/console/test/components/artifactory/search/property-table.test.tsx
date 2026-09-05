import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { PropertyTable } from '@konfigyr/components/artifactory/search/property-table';

import type { PropertyDefinition } from '@konfigyr/hooks/artifactory/types';
import type { PageResponse } from '@konfigyr/hooks/hateoas/types';

function page(data: Array<PropertyDefinition>, overrides: Partial<PageResponse<PropertyDefinition>['metadata']> = {}): PageResponse<PropertyDefinition> {
  return { data, metadata: { number: 1, size: 20, total: data.length, pages: 1, ...overrides } };
}

const timeout: PropertyDefinition = {
  id: 'property-example-timeout',
  key: 'com.example:public-service',
  owner: { id: '2', slug: 'konfigyr' },
  checksum: 'checksum-example-timeout',
  occurrences: 2,
  firstSeen: '1.0.0',
  lastSeen: '2.0.0',
  name: 'example.timeout',
  typeName: 'java.time.Duration',
  schema: { type: 'string', format: 'duration' },
  description: 'Timeout duration for example service calls.',
  defaultValue: '30s',
};

// A different artifact defining a property with the same name, to guard against duplicate list keys
// (PropertyTable keys list items by `id`, not `name`, since names collide across artifacts).
const otherArtifactTimeout: PropertyDefinition = {
  ...timeout,
  id: 'property-other-timeout',
  key: 'io.johndoe:notes-service',
  owner: { id: '1', slug: 'john-doe' },
};

describe('components | search | <PropertyTable/>', () => {
  afterEach(() => cleanup());

  test('should render every property in the response', () => {
    const { getByText } = renderComponentWithRouter(
      <PropertyTable properties={page([timeout, otherArtifactTimeout])}/>,
    );

    expect(getByText('com.example:public-service')).toBeInTheDocument();
    expect(getByText('io.johndoe:notes-service')).toBeInTheDocument();
  });

  test('should render an empty state when no properties are found', () => {
    const { getByText } = renderComponentWithRouter(
      <PropertyTable properties={page([])}/>,
    );

    expect(getByText('No matching properties found')).toBeInTheDocument();
  });

  test('should not render pagination controls for a single page of results', () => {
    const { queryByRole } = renderComponentWithRouter(
      <PropertyTable properties={page([timeout])}/>,
    );

    expect(queryByRole('navigation')).not.toBeInTheDocument();
  });

  test('should render pagination controls when there is more than one page of results', () => {
    const { getByRole } = renderComponentWithRouter(
      <PropertyTable properties={page([timeout], { pages: 3, total: 45 })} page={1} size={20}/>,
    );

    expect(getByRole('navigation')).toBeInTheDocument();
  });
});
