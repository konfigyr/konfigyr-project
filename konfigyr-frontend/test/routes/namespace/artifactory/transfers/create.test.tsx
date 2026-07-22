import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | transfers | create', () => {
  afterEach(() => cleanup());

  test('should render the request form', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/artifactory/transfers/create');

    await waitFor(() => {
      expect(getByRole('combobox', { name: 'Group Id' })).toBeInTheDocument();
      expect(getByText('Select a groupId first.')).toBeInTheDocument();
    });
  });

  test('should pre-fill the form from search params', async () => {
    const { findByRole, getByRole } = renderWithRouter(
      '/namespace/konfigyr/artifactory/transfers/create?groupId=com.acme.transfer-ready&fromNamespace=ebf',
    );

    expect(await findByRole('combobox', { name: 'Group Id' })).toHaveValue('com.acme.transfer-ready');

    await waitFor(() => {
      expect(getByRole('radio', { name: 'ebf' })).toBeChecked();
    });
  });

  test('should show a clear inline error when the backend rejects the request', async () => {
    const user = userEvent.setup();
    const { getByRole, getByText, router } = renderWithRouter(
      '/namespace/konfigyr/artifactory/transfers/create?groupId=unverified.group&fromNamespace=ebf',
    );

    await waitFor(() => {
      expect(getByRole('combobox', { name: 'Group Id' })).toHaveValue('unverified.group');
      expect(getByRole('radio', { name: 'ebf' })).toBeChecked();
    });

    await user.click(getByRole('button', { name: 'Request transfer' }));

    await waitFor(() => {
      expect(getByText('GroupId \'unverified.group\' is not verified for publishing')).toBeInTheDocument();
    });

    expect(router.state.location.pathname).toBe('/namespace/konfigyr/artifactory/transfers/create');
  });
});
