import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups | detail', () => {
  afterEach(() => cleanup());

  test('should render the group claim detail page', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups/com.example.group/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'com.example.group' })).toBeInTheDocument();
      expect(getByText('Active')).toBeInTheDocument();
    });
  });
});
