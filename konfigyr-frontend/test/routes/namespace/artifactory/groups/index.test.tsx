import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups', () => {
  afterEach(() => cleanup());

  test('should render the group claims list for a populated namespace', async () => {
    const { getAllByText, getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Claim a groupId' })).toBeInTheDocument();
      expect(getByText('com.example.group')).toBeInTheDocument();
      expect(getByText('io.github.acme')).toBeInTheDocument();
      expect(getByText('com.acme.transfer-ready')).toBeInTheDocument();
      expect(getAllByText('Active')).toHaveLength(2);
      expect(getByText('Pending')).toBeInTheDocument();
    });
  });

  test('should render an empty state when no group claims exist', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/john-doe/artifactory/groups');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Claim a groupId' })).toBeInTheDocument();
      expect(getByText('No group claims found')).toBeInTheDocument();
      expect(getByText('This namespace has not claimed any Maven groupId coordinates yet.')).toBeInTheDocument();
    });
  });
});
