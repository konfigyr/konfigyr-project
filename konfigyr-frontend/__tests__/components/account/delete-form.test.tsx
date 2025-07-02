import type { AccountAction } from 'konfigyr/app/settings/actions';

import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { AccountContextProvider, AccountDeleteForm } from 'konfigyr/components/account';
import messages from '../../../messages/en.json';

const account = { id: 'test-account', email: 'john.doe@konfigyr.com', fullName: 'John Doe' };

function TestForm({ action }: { action: AccountAction}) {
  return (
    <NextIntlClientProvider locale="en" messages={messages}>
      <AccountContextProvider account={account}>
        <AccountDeleteForm action={action} />
      </AccountContextProvider>
    </NextIntlClientProvider>
  );
}

describe('components/account/delete-form', () => {
  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render account delete form', async () => {
    const action = vi.fn();
    render(<TestForm action={action} />);

    expect(screen.getByText('Delete account')).toBeDefined();
    expect(screen.getByText(
      'This will nuke your account and all your data—no going back. Only do this if you\'re 100% sure. Seriously.',
    )).toBeDefined();
    expect(screen.getByRole('button')).toBeDefined();
    expect(screen.getByRole('button')).toHaveTextContent('Delete my account');

    expect(action).not.toBeCalled();
  });

  test('should show delete account confirmation modal and close it', async () => {
    const action = vi.fn();
    render(<TestForm action={action} />);

    await fireEvent.click(screen.getByRole('button'));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveAccessibleName('Are you sure?');
    expect(screen.getByRole('dialog')).toHaveAccessibleDescription(
      'This action cannot be undone. This will permanently delete your account and remove your data from our servers.',
    );

    expect(screen.getByRole('button', { name: 'Nope, I have changed my mind' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Yes, I am sure' })).toBeDefined();

    await fireEvent.click(screen.getByRole('button', { name: 'Nope, I have changed my mind' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    expect(action).not.toBeCalled();
  });

  test('should show delete account confirmation modal and confirm action', async () => {
    const action = vi.fn();
    render(<TestForm action={action} />);

    await fireEvent.click(screen.getByRole('button'));
    await fireEvent.click(screen.getByRole('button', { name: 'Yes, I am sure' }));

    expect(action).toBeCalled();
  });

});
