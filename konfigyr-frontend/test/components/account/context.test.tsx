import { describe, expect, test } from 'vitest';
import { AccountProvider } from '@konfigyr/components/account/context';
import { useAccount } from '@konfigyr/hooks';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client.js';
import { waitFor } from '@testing-library/react';

function AccountInformation() {
  const account = useAccount();

  return (
    <p>{account.fullName}</p>
  );
}

describe('components | account | <AccountProvider />', () => {
  test('render the account provider', async () => {
    const { getByText } = renderWithQueryClient((
      <AccountProvider>
        <AccountInformation/>
      </AccountProvider>
    ));

    await waitFor(() => {
      expect(getByText('Loading...')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(getByText('John Doe')).toBeInTheDocument();
    });
  });
});
