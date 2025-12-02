import { afterEach, beforeEach, describe, expect, test } from 'vitest';
import { AccountProvider } from '@konfigyr/components/account/context';
import { NamespaceProvider } from '@konfigyr/components/namespace/context';
import { accountKeys, getLastUsedNamespace, useNamespace } from '@konfigyr/hooks';
import { createTestQueryClient, renderWithQueryClient } from '@konfigyr/test/helpers/query-client.js';
import { cleanup, waitFor } from '@testing-library/react';

import type { Account, Namespace } from '@konfigyr/hooks/types';

function NamespaceInformation() {
  const namespace = useNamespace();

  return (
    <p>{namespace.slug}</p>
  );
}

describe('components | namespace | <NamespaceProvider />', () => {
  const queryClient = createTestQueryClient();

  const account: Account = {
    id: '06Y7W2BYKG9B9',
    email: 'john.doe@konfigyr.com',
    memberships: [],
  };

  const namespace: Namespace = {
    id: 'test-namespace',
    slug: 'konfigyr',
    name: 'Konfigyr project namespace',
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
        <NamespaceProvider namespace={namespace}>
          <NamespaceInformation/>
        </NamespaceProvider>
      </AccountProvider>
    ),  { queryClient });

    await waitFor(() => {
      expect(getByText(namespace.slug)).toBeInTheDocument();
    });

    expect(getLastUsedNamespace(account)).toBe(namespace.slug);
  });
});
