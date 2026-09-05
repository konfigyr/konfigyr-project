import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups', () => {
  afterEach(() => cleanup());

  test('should render the group claims list', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Claim a groupId' })).toBeInTheDocument();
      expect(getByText('com.example.group')).toBeInTheDocument();
    });
  });
});
