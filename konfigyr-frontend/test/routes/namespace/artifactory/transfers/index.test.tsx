import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | transfers', () => {
  afterEach(() => cleanup());

  test('should render the incoming transfers list by default', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/transfers');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Request transfer' })).toBeInTheDocument();
      expect(getByText('com.example.group')).toBeInTheDocument();
      expect(getByText('ebf')).toBeInTheDocument();
      expect(getByText('Pending')).toBeInTheDocument();
    });
  });

  test('should toggle to the outgoing transfers list', async () => {
    const { getByRole, getByText, queryByText } = renderWithRouter('/namespace/konfigyr/artifactory/transfers');

    await waitFor(() => {
      expect(getByText('com.example.group')).toBeInTheDocument();
    });

    const outgoingToggle = getByRole('link', { name: 'Outgoing' });
    outgoingToggle.click();

    await waitFor(() => {
      expect(getByText('io.github.acme')).toBeInTheDocument();
    });

    expect(queryByText('com.example.group')).not.toBeInTheDocument();
  });

  test('should render an empty state when no transfers exist', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/john-doe/artifactory/transfers');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Request transfer' })).toBeInTheDocument();
      expect(getByText('No ownership transfers found')).toBeInTheDocument();
    });
  });
});
