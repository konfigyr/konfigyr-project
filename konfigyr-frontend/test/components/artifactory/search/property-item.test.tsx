import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PropertyItem } from '@konfigyr/components/artifactory/search/property-item';

import type { PropertyDefinition } from '@konfigyr/hooks/artifactory/types';

const property: PropertyDefinition = {
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

describe('components | search | <PropertyItem/>', () => {
  afterEach(() => cleanup());

  test('should render the property name, type and description', () => {
    const { getByText } = renderWithMessageProvider(<PropertyItem property={property}/>);

    expect(getByText('example.timeout')).toBeInTheDocument();
    expect(getByText('java.time.Duration')).toBeInTheDocument();
    expect(getByText('Timeout duration for example service calls.')).toBeInTheDocument();
  });

  test('should show the owning artifact coordinates and namespace as plain text, not a link', () => {
    const { getByText, queryByRole } = renderWithMessageProvider(<PropertyItem property={property}/>);

    expect(getByText('com.example:public-service')).toBeInTheDocument();
    expect(getByText('konfigyr')).toBeInTheDocument();
    expect(queryByRole('link')).not.toBeInTheDocument();
  });

  test('should hide the owning artifact and owner attribution in the version variant', () => {
    const { queryByText } = renderWithMessageProvider(<PropertyItem property={property} variant="version"/>);

    expect(queryByText('com.example:public-service')).not.toBeInTheDocument();
    expect(queryByText('konfigyr')).not.toBeInTheDocument();
  });
});
