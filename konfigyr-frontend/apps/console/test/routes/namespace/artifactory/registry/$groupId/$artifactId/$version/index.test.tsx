import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | registry | version detail', () => {
  afterEach(() => cleanup());

  test('should render the artifact version detail page', async () => {
    const { getByRole } = renderWithRouter('/namespace/konfigyr/artifactory/registry/com.example/public-service/2.0.0/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'com.example:public-service:2.0.0' })).toBeInTheDocument();
    });
  });

  test('should render a 404 state for an unknown version', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/artifactory/registry/com.example/public-service/9.9.9/');

    await waitFor(() => {
      expect(getByText('Not found')).toBeInTheDocument();
    });
  });
});
