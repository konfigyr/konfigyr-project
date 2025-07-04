import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { useAccount, AccountAvatar, AccountContextProvider } from 'konfigyr/components/account';

function AccountInformation() {
  const [account] = useAccount();

  return (
    <AccountAvatar
      picture={account?.avatar}
      name={account?.fullName || 'Unknown'}
    />
  );
}

describe('components/account', () => {
  afterEach(() => cleanup());

  test('should render unknown account name when account context is not set', async () => {
    render(
      <AccountContextProvider>
        <AccountInformation />
      </AccountContextProvider>,
    );

    expect(screen.getByText('Unknown')).toBeDefined();
  });

  test('should render account name when account context is set', async () => {
    const account = { id: 'test-account', email: 'john.doe@konfigyr.com', fullName: 'John Doe', picture: '' };

    render(
      <AccountContextProvider account={account}>
        <AccountInformation />
      </AccountContextProvider>,
    );

    expect(screen.getByText('John Doe')).toBeDefined();
  });
});
