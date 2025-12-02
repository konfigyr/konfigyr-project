import { afterEach, describe, expect, test } from 'vitest';
import { AccountProvider } from '@konfigyr/components/account/context';
import { useAccount } from '@konfigyr/hooks';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client.js';
import { cleanup, waitFor } from '@testing-library/react';

function AccountInformation() {
  const account = useAccount();

  return (
    <p>{account.fullName}</p>
  );
}

describe('components | account | <AccountProvider />', () => {
  afterEach(() => cleanup());

  test('render the account provider', async () => {
    const { getByText } = renderWithQueryClient((
      <AccountProvider>
        <AccountInformation/>
      </AccountProvider>
    ));

    expect(getByText('Konfigyr')).toBeInTheDocument();
    expect(getByText('Configuration made easy.')).toBeInTheDocument();
    expect(getByText('Loading your account information...')).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('John Doe')).toBeInTheDocument();
    });
  });
});
