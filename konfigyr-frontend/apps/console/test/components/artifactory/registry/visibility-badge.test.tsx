import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ArtifactVisibilityBadge } from '@konfigyr/components/artifactory/registry/visibility-badge';

describe('components | registry | <ArtifactVisibilityBadge/>', () => {
  afterEach(() => cleanup());

  test('should render the Public label for a PUBLIC artifact', () => {
    const { getByText } = renderWithQueryClient(<ArtifactVisibilityBadge visibility="PUBLIC"/>);
    expect(getByText('Public')).toBeInTheDocument();
  });

  test('should render the Private label for a PRIVATE artifact', () => {
    const { getByText } = renderWithQueryClient(<ArtifactVisibilityBadge visibility="PRIVATE"/>);
    expect(getByText('Private')).toBeInTheDocument();
  });
});
