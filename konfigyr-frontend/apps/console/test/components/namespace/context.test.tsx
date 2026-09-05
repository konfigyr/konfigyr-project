import { afterEach, beforeEach, describe, expect, test } from 'vitest';
import { AccountProvider } from '@konfigyr/components/account/context';
import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { accountKeys, getLastUsedNamespace, namespaceKeys, useNamespace } from '@konfigyr/hooks';
import { createTestQueryClient, renderWithQueryClient } from '@konfigyr/test/helpers/query-client.js';
import { accounts, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';

import type { Account } from '@konfigyr/hooks/types';

function NamespaceInformation() {
  const namespace = useNamespace();

  return (
    <p>{namespace.slug}</p>
  );
}

describe('components | namespace | <NamespaceProvider />', () => {
  const queryClient = createTestQueryClient();

  const account: Account = { ...accounts.johnDoe };

  beforeEach(() => {
    queryClient.setQueryData(accountKeys.getAccount(), account);
    queryClient.setQueryData(namespaceKeys.getNamespaces(), [namespaces.konfigyr]);
  });

  afterEach(() => {
    queryClient.clear();
    cleanup();
  });

  test('render the namespace provider and save last used namespace value', async () => {
    const { getByText } = renderWithQueryClient((
      <AccountProvider>
        <NamespaceProvider namespace={namespaces.konfigyr}>
          <NamespaceInformation/>
        </NamespaceProvider>
      </AccountProvider>
    ), { queryClient });

    await waitFor(() => {
      expect(getByText(namespaces.konfigyr.slug)).toBeInTheDocument();
    });

    expect(getLastUsedNamespace(account, [namespaces.konfigyr, namespaces.johnDoe])).toBe(namespaces.konfigyr.slug);
  });
});
