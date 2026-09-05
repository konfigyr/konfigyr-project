import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | registry | detail', () => {
  afterEach(() => cleanup());

  test('should render the artifact overview with its versions', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/registry/com.example/public-service/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'com.example:public-service' })).toBeInTheDocument();
      expect(getByText('2.0.0')).toBeInTheDocument();
      expect(getByText('1.0.0')).toBeInTheDocument();
    });
  });

  test('should render a 404 state for unknown coordinates', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/artifactory/registry/unknown.group/unknown-artifact/');

    await waitFor(() => {
      expect(getByText('Not found')).toBeInTheDocument();
    });
  });
});
