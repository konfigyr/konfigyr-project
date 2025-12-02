import { afterAll, beforeAll, describe, expect, test, vi } from 'vitest';
import { AccountDeleteConfirmationDialog } from '@konfigyr/components/account/delete-dialog';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { accounts } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | account | <AccountDeleteConfirmationDialog/>', () => {
  const onDelete = vi.fn();
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <AccountDeleteConfirmationDialog account={accounts.johnDoe} onDelete={onDelete} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render account delete confirmation card', () => {
    expect(result.getByRole('button')).toBeInTheDocument();
    expect(result.getByRole('button')).toHaveAccessibleName('Delete account');
    expect(result.getByRole('paragraph')).toHaveTextContent(
      'This will nuke your account and all your data, there is no going back. Only do this if you\'re 100% sure. Seriously.',
    );

    expect(result.queryAllByRole('alertdialog')).to.have.lengthOf(0);
  });

  test('should open account delete confirmation dialog', async () => {
    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('alertdialog')).toBeInTheDocument();
      expect(result.getByRole('alertdialog')).toHaveAccessibleName('Are you sure?');
      expect(result.getByRole('alertdialog')).toHaveAccessibleDescription(
        'This action cannot be undone. This will permanently delete your account and remove your data from our servers.',
      );
    });
  });

  test('should close account delete confirmation dialog', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Nope, I have changed my mind' }),
    );

    await waitFor(() => {
      expect(result.queryAllByRole('alertdialog')).to.have.lengthOf(0);
    });
  });

  test('should fail to delete account due to unexpected error', async () => {
    onDelete.mockImplementationOnce(() => {
      return Promise.reject(new Error('Fail to delete account'));
    });

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeInTheDocument();
    });

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await waitFor(() => {
      expect(result.getByText('Unexpected server error occurred')).toBeInTheDocument();
    });
  });

  test('should successfully delete account', async () => {
    await waitFor(() => {
      expect(result.getByRole('alertdialog')).toBeInTheDocument();
    });

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );
  });
});
