import { afterEach, beforeEach, describe, expect, test } from 'vitest';
import { AccountProvider } from '@konfigyr/components/account/context';
import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { accountKeys, getLastUsedNamespace, useNamespace } from '@konfigyr/hooks';
import { NamespaceRole } from '@konfigyr/hooks/namespace/types';
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

  const account: Account = {
    ...accounts.johnDoe,
    memberships: [{
      id: namespaces.konfigyr.id,
      namespace: namespaces.konfigyr.slug,
      name: namespaces.konfigyr.name,
      role: NamespaceRole.ADMIN,
      since: '2025-12-01',
    }],
  };

  beforeEach(() => {
    queryClient.setQueryData(accountKeys.getAccount(), account);
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
    ),  { queryClient });

    await waitFor(() => {
      expect(getByText(namespaces.konfigyr.slug)).toBeInTheDocument();
    });

    expect(getLastUsedNamespace(account)).toBe(namespaces.konfigyr.slug);
  });
});
