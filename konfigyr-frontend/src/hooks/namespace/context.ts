import { createContext, useContext, useMemo } from 'react';
import { useAccountContext } from '@konfigyr/hooks/account/context';
import { useLocalStorage } from '@konfigyr/hooks/local-storage';

import type { Account, Namespace } from '@konfigyr/hooks/types';
import type { LocalStorageState } from '@konfigyr/hooks/local-storage';

export const NamespaceContext = createContext<Namespace | null>(null);

export const useNamespace = () => {
  const namespace = useContext(NamespaceContext);

  if (namespace === null) {
    throw new Error('Could not resolve the Namespace form the Namespace context provider');
  }

  return namespace;
};

export const generateLastUsedNamespaceKey = (account: Account) => `namespace.${account.id}`;

const isMemberOfNamespace = (memberships: Array<Namespace>, namespace: string) => {
  return memberships.some((membership) => membership.slug === namespace);
};

/**
 * Method that would return the last used namespace slug for the currently authenticated user account
 * from the local storage. If there is no such value in the storage, it would return `null`.
 */
export const getLastUsedNamespace = (account: Account, memberships: Array<Namespace>): string | null => {
  const item = window.localStorage.getItem(generateLastUsedNamespaceKey(account));

  if (typeof item === 'string') {
    const slug = JSON.parse(item);

    if (isMemberOfNamespace(memberships, slug)) {
      return slug;
    }
  }

  return null;
};

/**
 * Hook that returns the last used namespace slug for the currently authenticated user account
 * from the local storage. If there is no such namespace in the storage, it would return the first namespace
 * slug from the account namespace memberships.
 *
 * In case the account is not a member of any namespace, this hook would return `null` as a last namespace value.
 */
export const useLastUsedNamespace = (): LocalStorageState<string> => {
  const { account, memberships } = useAccountContext();
  const key = useMemo(() => generateLastUsedNamespaceKey(account), [account]);
  const [slug, setNamespace] = useLocalStorage<string>(key);

  if (typeof slug === 'string' && isMemberOfNamespace(memberships, slug)) {
    return [slug, setNamespace];
  }

  if (memberships.length > 0) {
    return [memberships[0].slug, setNamespace];
  }

  return [null, setNamespace];
};
