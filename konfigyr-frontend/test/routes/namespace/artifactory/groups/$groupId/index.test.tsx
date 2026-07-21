import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | groups | detail', () => {
  afterEach(() => cleanup());

  test('should render an active group claim detail page', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups/com.example.group/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'com.example.group' })).toBeInTheDocument();
      expect(getByText('Ownership verified')).toBeInTheDocument();
      expect(getByText('Group verification details')).toBeInTheDocument();
      expect(getByText('Active')).toBeInTheDocument();
      expect(getByRole('button', { name: 'Revoke claim' })).toBeInTheDocument();
    });
  });

  test('should render a pending group claim detail page', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups/io.github.acme/');

    await waitFor(() => {
      expect(getByRole('heading', { name: 'io.github.acme' })).toBeInTheDocument();
      expect(getByText('Claim pending')).toBeInTheDocument();
      expect(getByText('Create the marker repository for io.github.acme on GitHub, then re-check this claim.')).toBeInTheDocument();
      expect(getByText('Pending')).toBeInTheDocument();
      expect(getByRole('button', { name: 'Cancel claim' })).toBeInTheDocument();
      expect(getByRole('button', { name: 'Verify claim' })).toBeInTheDocument();
    });
  });

  test('should not render a conflict banner when there are no conflicting owners', async () => {
    const { getByText, queryByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups/com.example.group/');

    await waitFor(() => {
      expect(getByText('Ownership verified')).toBeInTheDocument();
    });

    expect(queryByText('Other namespaces already own artifacts here')).not.toBeInTheDocument();
  });

  test('should surface conflicting owners on the detail page', async () => {
    const { getAllByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/groups/com.acme.widgets/');

    await waitFor(() => {
      expect(getByText('Other namespaces already own artifacts here')).toBeInTheDocument();
      expect(getByText(
        'Namespace ebf already publishes artifacts under this groupId. You may need to request an ownership transfer before you can publish.',
      )).toBeInTheDocument();
    });

    expect(getAllByRole('alert')).toHaveLength(2);
  });
});
